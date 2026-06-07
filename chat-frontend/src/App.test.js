import { render, screen } from '@testing-library/react';
import App from './App';

test('renders chat app shell', () => {
  render(<App />);
  expect(screen.getByText(/AI Friend/i)).toBeInTheDocument();
  expect(screen.getByText(/Start a conversation with your AI friend/i)).toBeInTheDocument();
});
