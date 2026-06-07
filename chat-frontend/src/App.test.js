import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import App from './App';

beforeEach(() => {
  global.fetch = jest.fn();
});

afterEach(() => {
  jest.restoreAllMocks();
});

test('renders chat app shell', () => {
  render(<App />);
  expect(screen.getByRole('heading', { name: 'AI Friend' })).toBeInTheDocument();
  expect(screen.getByText(/Start a conversation with your AI friend/i)).toBeInTheDocument();
});

test('sends a message to the v1 chat API and renders the answer', async () => {
  global.fetch.mockResolvedValue({
    ok: true,
    json: async () => ({
      sessionId: 'session-1',
      answer: 'A grounded wellness answer.',
      safetyStatus: 'OK',
      citations: [],
      toolCalls: [],
      createdAt: '2026-06-07T00:00:00Z'
    })
  });

  render(<App />);

  await userEvent.type(screen.getByPlaceholderText(/type your message/i), 'What can I eat before my period?');
  await userEvent.click(screen.getByRole('button', { name: /send message/i }));

  expect(await screen.findByText('A grounded wellness answer.')).toBeInTheDocument();
  expect(global.fetch).toHaveBeenCalledWith(
    'http://localhost:8080/v1/chat/messages',
    expect.objectContaining({
      method: 'POST',
      headers: expect.objectContaining({
        'Content-Type': 'application/json',
        'X-AIF-Tenant-Key': 'dev-aif-demo-key'
      }),
      body: expect.any(String)
    })
  );
  expect(JSON.parse(global.fetch.mock.calls[0][1].body)).toEqual(expect.objectContaining({
    externalUserId: 'demo-user',
    message: 'What can I eat before my period?',
    scopes: ['demo']
  }));
});

test('renders an error message when the chat API fails', async () => {
  jest.spyOn(console, 'error').mockImplementation(() => {});
  global.fetch.mockResolvedValue({
    ok: false,
    status: 500
  });

  render(<App />);

  await userEvent.type(screen.getByPlaceholderText(/type your message/i), 'Hello');
  await userEvent.click(screen.getByRole('button', { name: /send message/i }));

  expect(await screen.findByText(/there was an error processing your request/i)).toBeInTheDocument();
});
