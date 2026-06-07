import React, { useState, useRef, useEffect } from 'react';
import { Container, Paper, TextField, IconButton, Typography, Box, Avatar, CircularProgress } from '@mui/material';
import SendIcon from '@mui/icons-material/Send';
import EmojiEmotionsIcon from '@mui/icons-material/EmojiEmotions';
import PersonIcon from '@mui/icons-material/Person';

const API_BASE_URL = process.env.REACT_APP_AIF_API_BASE_URL || 'http://localhost:8080';
const TENANT_API_KEY = process.env.REACT_APP_AIF_TENANT_API_KEY || 'dev-aif-demo-key';
const EXTERNAL_USER_ID = process.env.REACT_APP_AIF_EXTERNAL_USER_ID || 'demo-user';

function App() {
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [sessionId, setSessionId] = useState(null);
  const messagesEndRef = useRef(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView?.({ behavior: "smooth" });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const handleSend = async () => {
    if (!input.trim()) return;

    const userMessage = input;
    setInput('');
    setMessages(prev => [...prev, { text: userMessage, sender: 'user' }]);
    setIsLoading(true);

    try {
      const response = await fetch(`${API_BASE_URL}/v1/chat/messages`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-AIF-Tenant-Key': TENANT_API_KEY
        },
        body: JSON.stringify({
          externalUserId: EXTERNAL_USER_ID,
          sessionId,
          message: userMessage,
          locale: navigator.language || 'en-US',
          scopes: ['demo']
        })
      });

      if (!response.ok) {
        throw new Error(`Chat request failed with status ${response.status}`);
      }

      const data = await response.json();
      setSessionId(data.sessionId);
      setMessages(prev => [...prev, { text: data.answer, sender: 'ai' }]);
    } catch (error) {
      console.error('Error:', error);
      setMessages(prev => [...prev, { text: 'Sorry, there was an error processing your request.', sender: 'ai' }]);
    } finally {
      setIsLoading(false);
    }
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

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
                onKeyDown={handleKeyDown}
                placeholder="Type your message..."
                disabled={isLoading}
                variant="outlined"
                sx={{
                  '& .MuiOutlinedInput-root': {
                    borderRadius: 3
                  }
                }}
              />
              <IconButton 
                aria-label="Send message"
                color="primary" 
                onClick={handleSend} 
                disabled={isLoading || !input.trim()}
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
