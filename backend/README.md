# Investment Dashboard - Claude AI Integration

This is the backend for the Claude AI integration within the Investment Dashboard application. It provides an API endpoint that communicates with Anthropic's Claude AI to provide intelligent responses to user queries about investments and financial planning.

## Setup Instructions

### Prerequisites
- Node.js (v14 or later)
- npm (v6 or later)
- An Anthropic API key (from https://console.anthropic.com/)

### Installation

1. Clone the repository
2. Navigate to the backend directory:
   ```
   cd /path/to/investment-dashboard/backend
   ```
3. Install dependencies:
   ```
   npm install
   ```
4. Configure your environment:
   - Copy `.env.example` to `.env` (if not already done)
   - Add your Claude API key to the `.env` file:
     ```
     CLAUDE_API_KEY=your_actual_api_key_here
     ```

### Running the Backend

For development (with auto-restart on changes):
```
npm run dev
```

For production:
```
npm start
```

The server will start on port 3000 (or a different port if specified in the `.env` file).

## API Endpoints

### Claude AI Chat

**Endpoint:** `/api/claude-ai/chat`
**Method:** POST
**Body:**
```json
{
  "message": "What is the recommended stock allocation for someone retiring in 10 years?",
  "history": [
    {
      "role": "user",
      "content": "Previous message content",
      "timestamp": "2024-03-23T12:34:56.789Z"
    },
    {
      "role": "assistant",
      "content": "Previous assistant response",
      "timestamp": "2024-03-23T12:35:15.123Z"
    }
  ],
  "context": {
    "totalValue": 250000,
    "allocation": {
      "stocks": 60,
      "bonds": 30,
      "cash": 10
    },
    "performance": {
      "oneMonth": 2.5,
      "threeMonths": 4.2,
      "ytd": 7.8,
      "oneYear": 12.3
    }
  }
}
```

**Response:**
```json
{
  "message": "The Claude AI response..."
}
```

## Integration with Frontend

The Angular frontend communicates with this backend through the proxy configuration. The `/api/claude-ai` endpoint is routed to this Node.js backend, while other API endpoints go to the Spring Boot backend.

## Troubleshooting

If you encounter issues with the Claude AI integration:

1. Check that your API key is correct in the `.env` file
2. Ensure the backend server is running (`npm run dev`)
3. Check the server logs for any error messages
4. Verify that the proxy configuration is correctly set up in the Angular frontend

If Claude AI is unavailable, the backend will fall back to pre-defined responses based on keywords in the user's message. 