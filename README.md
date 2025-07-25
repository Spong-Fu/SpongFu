# SpongOut


## What is this Game?

It's a fast-paced, chaotic multiplayer physics game called **"Sumo Sponges"**. The goal is simple: be the last player on a shrinking circular platform. You control a "sponge" that automatically grows larger and more powerful the longer you wait to move. The game is designed for hilarious, unpredictable moments and frantic tests of timing.

## How does it work?

1.  **Simple Controls, Chaotic Aiming:** You have only one button. Pressing it launches your sponge. However, your aim is determined by a pointer that is constantly spinning at high speed. You have to press the button at the exact right moment to go where you want.

2.  **Risk vs. Reward:** The longer you wait without moving, the bigger your sponge gets. A bigger sponge means a more powerful launch, capable of sending opponents flying. But it also makes you a bigger, slower target.

3.  **Last Sponge Standing:** You win a round by using your launch to collide with other players and knock them off the platform.

4.  **Sudden Death:** If a round lasts longer than 30 seconds, the platform itself starts to shrink, forcing the remaining players into a final, chaotic confrontation until only one is left.

The game runs entirely in a web browser, with a server managing all the physics and player positions to ensure the experience is fair and synchronized for everyone.

## How are we going to build it?

### Backend Developer (The World Builder)

Your role is to create the single, consistent reality of the game world. You will build a centralized server application that simulates all game logic and physics, ensuring a fair and synchronized experience for every player.

**Key Responsibilities:**

1.  **Establish the Foundation:**
    *   Initialize a Spring Boot application using Maven dependencies for Web, WebSocket, and Lombok.
    *   Configure a WebSocket endpoint (e.g., `/game-websocket`) and enable the STOMP message broker to manage communication channels.

2.  **Architect the Game Flow:**
    *   Implement a clear state machine for the game's lifecycle (`WAITING`, `COUNTDOWN`, `RUNNING`, `SUDDEN_DEATH`, `ROUND_OVER`).
    *   Develop a central `GameService` to manage timers and state transitions based on player count and game rules.

3.  **Construct the Simulation Engine:**
    *   Create a fixed-rate Game Loop using a `ScheduledExecutorService` (e.g., running a `tick()` method 30 times per second).
    *   **In each `tick()` cycle, your engine will systematically:**
        a.  Update player attributes like pointer `angle` and `mass`.
        b.  Process incoming player commands (the "expel" action) by applying forces to player velocities.
        c.  Simulate the physics interactions, including resolving all player-vs-player collisions and applying environmental effects like friction.
        d.  Update all player positions based on their newly calculated velocities.
        e.  Enforce game rules, such as shrinking the arena during `SUDDEN_DEATH` and checking for player eliminations.

4.  **Manage Data and Communication:**
    *   Define the core `Player` model (POJO) to hold all server-side data: `sessionId`, `nickname`, position, velocity, mass, etc.
    *   Implement a `@MessageMapping` controller to handle incoming client commands sent to `/app/game.action`.
    *   Use Spring's `SimpMessagingTemplate` to broadcast game information to clients:
        *   **`/topic/game.state/main`:** Continuously stream the complete game state (player positions, angles, arena size) after each `tick`.
        *   **`/topic/game.events/main`:** Publish notifications for discrete events like `PLAYER_JOINED`, `ROUND_WINNER`, and `COUNTDOWN`.

---

### Frontend Developer (The Experience Designer)

Your role is to bring the game world to life in the browser. You will create a responsive and visually engaging client that accurately presents the state of the game and provides players with a seamless way to interact with it.

**Key Responsibilities:**

1.  **Craft the User Interface:**
    *   Design the HTML structure, including a `<canvas>` for game rendering and a "lobby" view for nickname entry.
    *   Integrate the necessary libraries: `p5.js` for canvas drawing and `StompJs` for real-time networking.

2.  **Establish the Communication Link:**
    *   Implement the logic for connecting to the backend's WebSocket endpoint using `StompJs` when the player decides to join.
    *   Upon a successful connection, immediately subscribe the client to two key destinations:
        *   `/topic/game.state/main` for high-frequency game state updates.
        *   `/topic/game.events/main` for game flow notifications.

3.  **Implement Player Controls:**
    *   Create a simple and responsive input handler that listens for the `Spacebar` key press.
    *   On input, publish a single, well-defined message (`{"action": "expel"}`) to the `/app/game.action` destination to signal the player's intent to the server.

4.  **Render the Dynamic Scene:**
    *   Utilize the `p5.js` `draw()` function as your main rendering loop. This loop will read from a `latestGameState` variable that is continuously updated by your STOMP subscription.
    *   **In every frame, your `draw()` function will:**
        a.  Clear the canvas to prepare for the new frame.
        b.  Draw the game arena, using the `arenaRadius` from the server's data to ensure it animates correctly during Sudden Death.
        c.  Iterate through the `players` array received from the server.
        d.  For each player, render their sponge at their server-provided `x, y` coordinates, draw their aiming pointer based on the `angle`, and display their `nickname` as text above them.

5.  **Display Game Information:**
    *   Use the data from the `/topic/game.events/main` subscription to update the UI with contextual information.
    *   This includes displaying the pre-round countdown, announcing the round winner, and updating a persistent scoreboard that shows player rankings.
