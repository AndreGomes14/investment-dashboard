/**
 * Main entry point for the backend server
 */

// Import required packages
const express = require('express');
const cors = require('cors');
const bodyParser = require('body-parser');
require('dotenv').config();

// Import routes
const claudeAiRoutes = require('./routes/claude-ai.routes');

// Initialize express app
const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(cors());
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: true }));

// Log all incoming requests
app.use((req, res, next) => {
  console.log(`${new Date().toISOString()} - ${req.method} ${req.originalUrl}`);
  next();
});

// Routes
app.use('/api/claude-ai', claudeAiRoutes);

// Simple health check
app.get('/health', (req, res) => {
  res.status(200).json({ status: 'ok', message: 'Server is running' });
});

// Default route
app.get('/', (req, res) => {
  res.status(200).json({ 
    message: 'Investment Dashboard API', 
    endpoints: {
      claudeAi: '/api/claude-ai/chat'
    }
  });
});

// Start the server
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
  console.log(`Claude AI endpoint available at http://localhost:${PORT}/api/claude-ai/chat`);
}); 