# Spong-Fu

## TL;DR
PLAY HERE: **https://spong-fu.dev/**

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
// Add FE infos here
