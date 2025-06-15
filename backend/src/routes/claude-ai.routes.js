/**
 * Claude AI routes
 */
const express = require('express');
const router = express.Router();
const claudeAiController = require('../controllers/claude-ai.controller');

// Process a chat message with Claude AI
router.post('/chat', claudeAiController.processChat);

module.exports = router; 