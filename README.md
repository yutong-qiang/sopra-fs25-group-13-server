# THE CHAMELEON

## Introduction
The Chameleon is a web-based multiplayer social deduction game inspired by the popular party game of the same name. In each round, all players except one — the Chameleon — are shown a secret word. Players then take turns giving subtle clues related to the word, trying to show they know it without being too obvious. The Chameleon, who doesn't know the word, must improvise a clue and blend in. After all clues are given, everyone votes to find the imposter.
Our implementation is designed for seamless online play, featuring real-time interactions so players can see each other’s reactions and experience synchronized role assignment, hint and voting phases, and results. Unlike traditional versions, our app does not use a grid of words and instead only one secret word is selected per round to keep the game simple while maintaining a fun and engaging experience. We've also added other features like automatic round tracking and the leaderboard. The game is ideal for remote groups looking for face-to-face social deduction gameplay from anywhere.

## Technologies used
* **Frontend**: Next.js, React
* **Backend**: Java, Spring Boot, STOMP WebSocket (via SockJS)
* **Video API**: Twilio Video API
* **Deployment**: Google App Engine, Vercel

## High-level components
### [AppController.java](https://github.com/yutong-qiang/sopra-fs25-group-13-server/blob/main/src/main/java/ch/uzh/ifi/hase/soprafs24/controller/AppController.java)
Acts as the central REST controller for the backend. It handles all HTTP endpoints related to: User management (register, login, logout, avatar upload); Game session management (create, join, end, retrieve info); Leaderboard access

Interacts with:
* AppService for user authentication, game logic, and data retrieval
* TwilioService to generate video room tokens
* UserDTOMapper, GameDTOMapper for mapping entities to response objects
* GameSession, User, and Player entities via service layer

### [GameSessionController.java](https://github.com/yutong-qiang/sopra-fs25-group-13-server/blob/main/src/main/java/ch/uzh/ifi/hase/soprafs24/controller/GameSessionController.java)
Handles real-time player interactions over WebSocket during the game.

Interacts with:
* AppService to validate tokens and retrieve user/game data
* GameSessionService to process player actions during the game


### [WebSocketConfig.java](https://github.com/yutong-qiang/sopra-fs25-group-13-server/blob/main/src/main/java/ch/uzh/ifi/hase/soprafs24/config/WebSocketConfig.java)
Enables WebSocket messaging for the entire application and registers the /game-ws STOMP endpoint for frontend communication

Interacts with:
* GameSessionController by defining the message broker /game/topic to send real-time updates


### [TwilioService.java](https://github.com/yutong-qiang/sopra-fs25-group-13-server/blob/main/src/main/java/ch/uzh/ifi/hase/soprafs24/service/TwilioService.java)
Uses the Twilio Java SDK to manage room lifecycle (create, generate token, close room)

Interacts with:
* AppController to create Twilio video rooms and generate access tokens


## Launch & Deployment
### Prerequisites
Before starting, make sure you have:
* Java 17+
* Gradle
* Node.js (for frontend)
* External Dependency: Twilio credentials (TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, TWILIO_API_KEY, TWILIO_API_SECRET)
* Google Cloud SDK (for deployment)

### Setup
```
git clone https://github.com/yutong-qiang/sopra-fs25-group-13-server.git
```
```
cd server
```

### Build and run the application
```
./gradlew build
```
```
./gradlew bootRun
```

### Run tests
```
./gradlew test
```

### Deployment
* The main branch is automatically deployed onto Google Cloud's App Engine

## Roadmap
* Spectator Mode for non-players to watch ongoing games
* AI-powered hint suggestions for solo / async play
* Mobile layout optimization for better touch UX

## Authors and Acknowledgement
* Katie Jingxuan He - [cutie72](https://github.com/cutie72)
* Lorenzo Frigoli - [lorefrigo](https://github.com/lorefrigo)
* Luca Bärtschiger - [lucabarts](https://github.com/lucabarts)
* Yutong Qiang - [yutong-qiang](https://github.com/yutong-qiang)

Special thanks to our teaching assistant Lukas Niedhart for his guidance, support, and valuable feedback throughout the development process. 

We also appreciate the course Software Engineering Lab for providing a hands-on learning experience.

Thanks to everyone who helped us test the software and provided valuable suggestions.

## License
This project is licensed under the Apache License 2.0 – see the [LICENSE](https://github.com/yutong-qiang/sopra-fs25-group-13-server/blob/main/LICENSE) file for details.
