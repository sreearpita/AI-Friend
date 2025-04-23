# AI Friend Chat Application

A modern chat application that simulates conversations with an AI friend using Spring Boot and React. The AI is powered by Ollama and maintains a consistent persona of a long-time friend from school.

## Features

- 🤖 Personalized AI responses with context awareness
- 💬 Real-time chat interface
- 🎨 Modern, responsive UI design
- ⚡ Fast and efficient message handling
- 🔄 Smooth animations and transitions
- 📱 Mobile-friendly design

## Tech Stack

### Backend
- Java 17
- Spring Boot 3.2.3
- Spring AI
- Ollama AI Integration

### Frontend
- React
- Material-UI (MUI)
- Axios
- Modern CSS with gradients and animations

## Prerequisites

Before running the application, make sure you have the following installed:
- Java 17 or higher
- Node.js and npm
- Ollama (with mistral model installed)
- Maven

## Setup and Installation

### Backend Setup

1. Clone the repository:
```bash
git clone <repository-url>
cd <project-directory>
```

2. Install dependencies and build the project:
```bash
mvn clean install
```

3. Make sure Ollama is running and the mistral model is installed:
```bash
ollama pull mistral
ollama run mistral
```

4. Run the Spring Boot application:
```bash
mvn spring-boot:run
```

The backend will start on `http://localhost:8080`

### Frontend Setup

1. Navigate to the frontend directory:
```bash
cd chat-frontend
```

2. Install dependencies:
```bash
npm install
```

3. Start the development server:
```bash
npm start
```

The frontend will be available at `http://localhost:3000`

## Project Structure

```
.
├── src/                                # Backend source files
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/demo/
│   │   │       ├── controller/        # REST controllers
│   │   │       └── SpringbootAIApplication.java
│   │   └── resources/
│   │       └── application.properties # Application configuration
│   └── test/                         # Test files
├── chat-frontend/                    # React frontend
│   ├── src/
│   │   ├── App.js                   # Main React component
│   │   ├── index.js
│   │   └── index.css                # Global styles
│   ├── package.json
│   └── README.md
└── README.md
```

## Configuration

### Backend Configuration
The application.properties file contains important configurations:

```properties
server.port=8080
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.model=mistral
```

### Frontend Configuration
The frontend communicates with the backend at `http://localhost:8080`. This can be modified in `App.js` if needed.

## Usage

1. Start both the backend and frontend servers
2. Open your browser to `http://localhost:3000`
3. Start chatting with your AI friend!

## Features in Detail

- **Persistent Context**: The AI maintains the context of being your childhood friend from St. Antony's School
- **Natural Conversations**: Responses are tailored to feel like talking to a real friend
- **Real-time Feedback**: Visual indicators for message status and AI thinking
- **Responsive Design**: Works seamlessly on both desktop and mobile devices

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- Thanks to the Spring AI team for their excellent framework
- Thanks to the Ollama team for their AI model support
- Material-UI for the beautiful React components 