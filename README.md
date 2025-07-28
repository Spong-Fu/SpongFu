# Spong-Fu

<img width="512" height="512" alt="20250728_1539_Sponge Combat Logo_simple_compose_01k18m1rr6fggbhctzd4a4ajq5-min" src="https://github.com/user-attachments/assets/cc144bb1-175e-45a8-9684-45ffb37a1f3f" />

## TL;DR
PLAY HERE: **https://spong-fu.dev/**<br>
PRESS `SPACEBAR` TO WIN<br>
**STAR OUR REPO ;)**

## What is this Game?

It's simple fast-paced In-Browser **MULTIPLAYER** game born from chaos and too much black coffee.

## How Does it Work?

It’s fast, ridiculous, and deceptively skill-based.

1. **One Button, No Mercy:** You’ve got a single button. That’s it. Press it to launch your sponge. The catch? Your aim spins wildly, so timing is everything. Miss it—and you’re flying somewhere *very* unplanned.

2. **Grow Big or Die Fast:** Stay still and your sponge swells with power. Bigger sponge = stronger launch. But wait too long... and someone might send you flying first.

3. **Bumper Battle Royale:** Slam into other players to knock them off the platform. Last sponge standing wins. Simple? Yes. Calm? Absolutely not.

4. **The Floor is a Lie:** After 5 seconds of madness, the platform begins to shrink. Fast. Hope you aimed well.

Oh—and it all runs in your browser. No downloads. Just instant, synchronized chaos powered by a server that keeps things fair (even if nothing *feels* fair).

## How to run it locally
If you have docker - then:
```
docker run pmackiewicz6/spongout-image:latest
```
And go to `http://localhost:8080`

If not then:
### Requirements
Java 21

### Instructions
1. Clone repo
2. `cd` to dir
3. `./mvnw spring-boot:run`
4. And go to `http://localhost:8080`

## Some Technical Blabla

### Backend
Heart of this game is written in... **JAVA w/ Spring-Boot <3**
We got dependencies injections and more than 20 classes! Could be more, but didn't have time for that much **CLEAN CODE** :D
For communication Client <-> Server we used WebSockets w/ STOMP protocol over it.

#### How does it work on high level?
1. Player goes to https://spong-fu.dev/
2. Types nickname
3. this request goes to `GameLobbyService` (through `GameController`)
4. In this place, we create `GameInstance` from gathered players (2-5 players for each game)
6. And throw them right into `GameExecutionService` and this is the moment when **GAME STARTS!**
7. Here we store all currently played GameInstances and making them alive! Their `GameState` is updating 60x/s and it's sended to all subscribers (players)
8. In every `tick()` `GameInstance` goes from `GameExecutionService` to `GameEngine` for players positions updates & colissions checking. This is also the place where we get our players moving!
9. In every `tick()` we also check if someone was eliminated or game ended and we got a winner already. All this infos are sent to players via WebSockets to keep them updated.
10. If only one players is left - then we **got a WINNER and GAME is finished!**

### Frontend
The Frontend is written in... **Plain JavaScript**
We are using P5.js library for our canva and game design, and it's definitely not clean code frontend is also messy as hell, hope that's not a problem
And as written in Backend, we are using STOMP for our socket connections

#### How does it work?
1. Lobby & Connection: We connect to the socket with `initNetworking` and wait for the user to enter a nickname and click "Join Game". Clicking "Join Game" you'll create a lobby/ gameId.
2. Game Start: Now you wait, when someone else clicks join game, they join your lobby, now you can play with just the 2 of you or wait for others.
3. Player Input: You hit SPACEBAR to move which triggers the `keyPressed()` function.
4. Rendering: From the socket connection we receive coordinates in real time from `onPrivateMessage()` and that gives us all the updated states and we draw those states using p5.js.
5. Elimination and Victory: These are also handled through `onPrivateMessage()`, and the `FloatingText` class.
6. My first time writing a readme, so if it's not alright pls understand :)

# Have fun :)

## Worth to mention
<img width="1046" height="446" alt="image" src="https://github.com/user-attachments/assets/81a44334-bea5-40ca-aa04-645feb39dc3d" />
You can configure server settings and physics with `/src/main/resources/application.yaml` file.
