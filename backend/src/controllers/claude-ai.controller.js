/**
 * Controller for Claude AI API integration
 */

// Import the Anthropic SDK
const { Anthropic } = require('@anthropic-ai/sdk');
// Load environment variables
require('dotenv').config();

// Initialize Anthropic client with API key from environment variables
const anthropic = new Anthropic({ apiKey: process.env.CLAUDE_API_KEY });

// Set this to true to always use fallback responses (for testing without an API key)
const ALWAYS_USE_FALLBACK = false;

/**
 * Processes a chat message and returns a response from Claude AI
 * 
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 */
exports.processChat = async (req, res) => {
  try {
    const { message, history = [], context = null } = req.body;
    
    if (!message) {
      return res.status(400).json({ error: 'Message is required' });
    }

    // Construct context information from portfolio data
    let contextInfo = '';
    if (context) {
      contextInfo = `
The user has the following portfolio data:
- Total portfolio value: $${context.totalValue.toLocaleString()}
- Asset allocation: ${context.allocation.stocks}% stocks, ${context.allocation.bonds}% bonds, ${context.allocation.cash}% cash
- Performance: 1-month: ${context.performance.oneMonth}%, YTD: ${context.performance.ytd}%, 1-year: ${context.performance.oneYear}%

Based on this information, provide a helpful response to their question.`;
    }
    
    // Format previous conversation history for Claude
    const formattedHistory = history.map(msg => ({
      role: msg.role,
      content: msg.content
    }));
    
    // If ALWAYS_USE_FALLBACK is true, skip the API call and use fallback
    if (ALWAYS_USE_FALLBACK) {
      let fallbackResponse = getFallbackResponse(message, context);
      return res.json({
        message: fallbackResponse,
        note: "This is a fallback response. To use actual Claude AI, update your API key in the .env file."
      });
    }
    
    try {
      // Call Claude API with properly formatted messages
      const response = await anthropic.messages.create({
        model: "claude-3-sonnet-20240229",
        max_tokens: 1000,
        messages: [
          // Include formatted history
          ...formattedHistory.slice(-10), // Keep only the 10 most recent messages to stay within token limits
          // Add the user's new message
          { role: 'user', content: message }
        ],
        system: `You are a financial advisor AI assistant. ${contextInfo}
          Provide helpful, accurate advice about investments and financial planning.
          Your answers should be clear, concise, and educational.
          When discussing investment strategies, always remind users that past performance does not guarantee future results.
          Never provide specific investment recommendations for individual stocks or securities.
          Explain concepts in easy-to-understand language and use examples when helpful.`
      });
      
      // Extract the AI's response from the API response
      const aiResponse = response.content[0].text;
      
      // Send the response to the client
      return res.json({
        message: aiResponse
      });
    } catch (apiError) {
      console.error('Error calling Claude API:', apiError);
      
      // If there's an API error, fall back to mock responses
      let fallbackResponse = getFallbackResponse(message, context);
      
      return res.json({
        message: fallbackResponse,
        note: "This is a fallback response due to API issues. Please check your API key and try again later."
      });
    }
    
  } catch (error) {
    console.error('Error processing Claude AI request:', error);
    return res.status(500).json({
      error: 'An error occurred while processing your request. Please try again later.'
    });
  }
};

/**
 * Generate a fallback response based on keywords in the message
 * Used when the Claude API call fails
 */
function getFallbackResponse(message, context) {
  const msg = message.toLowerCase();
  
  if (msg.includes('portfolio')) {
    return `Based on your portfolio information, I can see you have a diversified mix with ${context?.allocation.stocks}% in stocks, ${context?.allocation.bonds}% in bonds, and ${context?.allocation.cash}% in cash. This is a fairly standard allocation that balances growth potential with risk management. Remember that portfolio diversification is a key strategy for managing investment risk.`;
  } else if (msg.includes('performance')) {
    return `Your portfolio has shown a ${context?.performance.oneYear}% return over the past year, which is solid performance. The one-month return is ${context?.performance.oneMonth}%. Remember that short-term fluctuations are normal, and it's important to focus on long-term investment goals.`;
  } else if (msg.includes('stock') || msg.includes('equity')) {
    return `Stocks (equities) make up ${context?.allocation.stocks}% of your portfolio. They typically offer higher potential returns but with increased volatility. They're an important component of long-term wealth building, especially when you have a longer investment horizon.`;
  } else if (msg.includes('bond')) {
    return `Bonds constitute ${context?.allocation.bonds}% of your portfolio. They generally provide more stable returns than stocks and can help balance portfolio risk. They're especially important as you get closer to needing to access your investment funds.`;
  } else if (msg.includes('cash')) {
    return `Your portfolio includes ${context?.allocation.cash}% in cash or cash equivalents. While cash doesn't generate significant returns, especially in low interest rate environments, it provides liquidity and stability to your portfolio.`;
  } else if (msg.includes('risk')) {
    return `With your current allocation of ${context?.allocation.stocks}% stocks, ${context?.allocation.bonds}% bonds, and ${context?.allocation.cash}% cash, your portfolio has a moderate risk profile. Risk management is about finding the right balance between potential returns and the level of volatility you're comfortable with.`;
  } else {
    return `Thank you for your question about "${message}". As your AI financial assistant, I'm here to help you understand your investments and financial options. If you have specific questions about your portfolio, performance, or investment strategies, please feel free to ask. Remember that all investment advice should be considered in the context of your overall financial goals and risk tolerance.`;
  }
} 