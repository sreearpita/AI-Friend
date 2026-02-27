import React, { useState, useRef, useEffect } from 'react';
import { Container, Paper, TextField, IconButton, Typography, Box, Avatar, CircularProgress } from '@mui/material';
import SendIcon from '@mui/icons-material/Send';
import EmojiEmotionsIcon from '@mui/icons-material/EmojiEmotions';
import PersonIcon from '@mui/icons-material/Person';
import axios from 'axios';

const API_BASE = process.env.REACT_APP_API_BASE || 'http://localhost:8080';

function getOrCreateUserId() {
  let userId = localStorage.getItem('ai_friend_user_id');
  if (!userId) {
    userId = crypto.randomUUID();
    localStorage.setItem('ai_friend_user_id', userId);
  }
  return userId;
}

async function createConversationId(userId) {
  const res = await axios.post(`${API_BASE}/api/conversations`, {}, {
    headers: { 'X-User-Id': userId }
  });
  const convId = res.data.conversationId;
  localStorage.setItem('ai_friend_conversation_id', convId);
  return convId;
}

async function getOrCreateConversationId(userId) {
  const stored = localStorage.getItem('ai_friend_conversation_id');
  if (stored) return stored;
  return createConversationId(userId);
}

function App() {
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [initError, setInitError] = useState(false);
  const [userId] = useState(() => getOrCreateUserId());
  const [conversationId, setConversationId] = useState(null);
  const messagesEndRef = useRef(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  useEffect(() => {
    getOrCreateConversationId(userId)
      .then(setConversationId)
      .catch(err => {
        console.error('Failed to initialize conversation:', err);
        setInitError(true);
      });
  }, [userId]);

  const handleSend = async () => {
    if (!input.trim() || !conversationId) return;

    const userMessage = input;
    setInput('');
    setMessages(prev => [...prev, { text: userMessage, sender: 'user' }]);
    setIsLoading(true);

    try {
      const response = await axios.post(`${API_BASE}/api/chat`, {
        conversationId,
        message: userMessage
      }, {
        headers: {
          'Content-Type': 'application/json',
          'X-User-Id': userId
        }
      });

      setMessages(prev => [...prev, { text: response.data.reply, sender: 'ai' }]);
    } catch (error) {
      console.error('Error:', error);
      // If the conversation was not found (e.g. DB wiped), create a new one and retry
      if (error.response && error.response.status === 404) {
        localStorage.removeItem('ai_friend_conversation_id');
        try {
          const newConvId = await createConversationId(userId);
          setConversationId(newConvId);
          setMessages(prev => [...prev, { text: 'Starting a fresh conversation. Please resend your message.', sender: 'ai' }]);
        } catch (retryErr) {
          setMessages(prev => [...prev, { text: 'Failed to reconnect. Please refresh the page.', sender: 'ai' }]);
        }
      } else {
        setMessages(prev => [...prev, { text: 'Sorry, there was an error processing your request.', sender: 'ai' }]);
      }
    } finally {
      setIsLoading(false);
    }
  };

  const handleKeyPress = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  if (initError) {
    return (
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '100vh' }}>
        <Typography color="error">
          Could not connect to the backend. Please make sure the server is running and refresh.
        </Typography>
      </Box>
    );
  }

  return (
    <Box 
      sx={{ 
        minHeight: '100vh', 
        background: 'linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%)',
        py: 2
      }}
    >
      <Container maxWidth="md" sx={{ height: 'calc(100vh - 32px)' }}>
        <Paper 
          elevation={6} 
          sx={{ 
            height: '100%', 
            display: 'flex', 
            flexDirection: 'column',
            borderRadius: 3,
            overflow: 'hidden'
          }}
        >
          <Box 
            sx={{ 
              p: 2, 
              borderBottom: 1, 
              borderColor: 'divider',
              background: 'linear-gradient(90deg, #4b6cb7 0%, #182848 100%)',
              color: 'white',
              display: 'flex',
              alignItems: 'center',
              gap: 2
            }}
          >
            <Avatar sx={{ bgcolor: 'rgba(255,255,255,0.2)' }}>
              <EmojiEmotionsIcon />
            </Avatar>
            <Typography variant="h5" fontWeight="medium">
              AI Friend
            </Typography>
          </Box>
          
          <Box sx={{ 
            flexGrow: 1, 
            overflowY: 'auto', 
            p: 2,
            display: 'flex',
            flexDirection: 'column',
            gap: 2,
            bgcolor: '#f8f9fa'
          }}>
            {messages.length === 0 && (
              <Box sx={{ 
                display: 'flex', 
                flexDirection: 'column', 
                alignItems: 'center', 
                justifyContent: 'center',
                height: '100%',
                opacity: 0.7
              }}>
                <EmojiEmotionsIcon sx={{ fontSize: 60, color: 'primary.main', mb: 2 }} />
                <Typography variant="h6" color="text.secondary">
                  Start a conversation with your AI friend
                </Typography>
              </Box>
            )}
            
            {messages.map((message, index) => (
              <Box
                key={index}
                sx={{
                  display: 'flex',
                  flexDirection: message.sender === 'user' ? 'row-reverse' : 'row',
                  gap: 1,
                  alignItems: 'flex-start'
                }}
              >
                <Avatar 
                  sx={{ 
                    bgcolor: message.sender === 'user' ? 'primary.main' : 'secondary.main',
                    width: 36,
                    height: 36
                  }}
                >
                  {message.sender === 'user' ? <PersonIcon /> : <EmojiEmotionsIcon />}
                </Avatar>
                <Paper
                  elevation={1}
                  sx={{
                    p: 2,
                    maxWidth: '70%',
                    borderRadius: 3,
                    bgcolor: message.sender === 'user' ? 'primary.main' : 'white',
                    color: message.sender === 'user' ? 'white' : 'text.primary',
                    position: 'relative',
                    '&:before': {
                      content: '""',
                      position: 'absolute',
                      top: 12,
                      [message.sender === 'user' ? 'right' : 'left']: -8,
                      width: 0,
                      height: 0,
                      borderTop: '8px solid transparent',
                      borderBottom: '8px solid transparent',
                      [message.sender === 'user' ? 'borderLeft' : 'borderRight']: `8px solid ${message.sender === 'user' ? '#1976d2' : 'white'}`
                    }
                  }}
                >
                  <Typography sx={{ whiteSpace: 'pre-wrap' }}>{message.text}</Typography>
                </Paper>
              </Box>
            ))}
            
            {isLoading && (
              <Box
                sx={{
                  display: 'flex',
                  flexDirection: 'row',
                  gap: 1,
                  alignItems: 'flex-start'
                }}
              >
                <Avatar 
                  sx={{ 
                    bgcolor: 'secondary.main',
                    width: 36,
                    height: 36
                  }}
                >
                  <EmojiEmotionsIcon />
                </Avatar>
                <Paper
                  elevation={1}
                  sx={{
                    p: 2,
                    borderRadius: 3,
                    bgcolor: 'white',
                    display: 'flex',
                    alignItems: 'center',
                    gap: 1
                  }}
                >
                  <CircularProgress size={16} />
                  <Typography>Thinking...</Typography>
                </Paper>
              </Box>
            )}
            <div ref={messagesEndRef} />
          </Box>

          <Box sx={{ p: 2, borderTop: 1, borderColor: 'divider', bgcolor: 'white' }}>
            <Box sx={{ display: 'flex', gap: 1 }}>
              <TextField
                fullWidth
                multiline
                maxRows={4}
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyPress={handleKeyPress}
                placeholder="Type your message..."
                disabled={isLoading || !conversationId}
                variant="outlined"
                sx={{
                  '& .MuiOutlinedInput-root': {
                    borderRadius: 3
                  }
                }}
              />
              <IconButton 
                color="primary" 
                onClick={handleSend} 
                disabled={isLoading || !input.trim() || !conversationId}
                sx={{ 
                  bgcolor: 'primary.main', 
                  color: 'white',
                  '&:hover': {
                    bgcolor: 'primary.dark'
                  },
                  '&.Mui-disabled': {
                    bgcolor: 'rgba(0, 0, 0, 0.12)',
                    color: 'rgba(0, 0, 0, 0.26)'
                  }
                }}
              >
                <SendIcon />
              </IconButton>
            </Box>
          </Box>
        </Paper>
      </Container>
    </Box>
  );
}

export default App;

