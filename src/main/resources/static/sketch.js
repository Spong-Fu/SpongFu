let client; 
let latestGameState = null; 
let latestGameEvent = null; 
let mySessionId = null; 


const arena = { w: 1200, h: 700 };

function setup() {
    let canvas = createCanvas(arena.w, arena.h);
    canvas.parent("game-container");
    
    const joinButton = document.getElementById('join-button');
    const nicknameInput = document.getElementById('nickname');
    const lobby = document.getElementById('lobby');
    const gameContainer = document.getElementById('game-container');

    joinButton.onclick = () => {
        const nickname = nicknameInput.value.trim();
        if (nickname) {
            lobby.style.display = 'none';
            gameContainer.style.display = 'block';
            initNetworking(nickname);
        } else {
            alert('Please enter a nickname!');
        }
    };
}

function draw() {
    background(34); 

    if (!latestGameState) {
        drawStatusText(latestGameEvent ? latestGameEvent.message : 'Connecting to server...');
        return;
    }

    push();
    translate(width / 2, height / 2);

    const arenaRadius = latestGameState.arenaRadius || 300;
    fill(100); 
    noStroke();
    ellipse(0, 0, arenaRadius * 2);

    latestGameState.players.forEach(player => {
        if (player.isAlive) {
            drawPlayer(player);
        }
    });
    pop();
    
    if (latestGameEvent) {
        if (latestGameEvent.type === 'COUNTDOWN' || latestGameEvent.type === 'ROUND_WINNER') {
            drawStatusText(latestGameEvent.message);
        }
    }
}

/**
 * Draws a single player object received from the server.
 * @param {object} player - The player data from the server.
 */

function drawPlayer(player) {
    // Draw the sponge (circle)
    fill(player.color || '#fff');
    noStroke();
    ellipse(player.position.x, player.position.y, player.mass * 2);

    fill(255);
    textAlign(CENTER, BOTTOM);
    textSize(14);
    text(player.nickname, player.position.x, player.position.y - player.mass - 5);

    if (player.sessionId === mySessionId) {
        push();
        translate(player.position.x, player.position.y);
        rotate(player.angle);
        stroke(255);
        strokeWeight(3);
        line(0, 0, player.mass + 20, 0); // Pointer length grows with mass
        pop();
    }
}

/**
 * Draws large text in the center of the screen for status updates.
 * @param {string} textToShow - The message to display.
 */
function drawStatusText(textToShow) {
    push();
    textAlign(CENTER, CENTER);
    textSize(64);
    fill("white");
    stroke(0);
    strokeWeight(4);
    text(textToShow, width / 2, height / 2);
    pop();
}

// --- Player Input ---

function keyPressed() {
    if (key === ' ' && client && client.active) {
        client.publish({
            destination: '/app/game.action',
            body: JSON.stringify({ action: 'expel' })
        });
    }
}

// --- Networking Logic ---

/**
 * Initializes the StompJs client and connects to the server.
 * @param {string} nickname - The player's chosen nickname.
 */
function initNetworking(nickname) {
    client = new StompJs.Client({
        brokerURL: 'ws://localhost:8080/ws',
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
        debug: msg => console.log('[STOMP]', msg),

        onConnect: (frame) => {
            console.log('STOMP client connected.');
            mySessionId = frame.headers['user-name'];
            console.log('My session ID is:', mySessionId);

            // --- Subscribe to Server Topics ---
            // 1. Subscribe to the main game state topic
            // This provides continuous updates of all player positions, etc.
            client.subscribe('/topic/game.state/main', (message) => {
                latestGameState = JSON.parse(message.body);
            });

            // 2. Subscribe to the game events topic
            // This provides notifications for specific events like countdowns or wins.
            client.subscribe('/topic/game.events/main', (message) => {
                latestGameEvent = JSON.parse(message.body);
                console.log("Game Event:", latestGameEvent);
            });
            
            // --- Join the Game ---
            // After connecting and subscribing, tell the server we want to join.
            client.publish({
                destination: '/app/game.action',
                body: JSON.stringify({ action: 'join', nickname: nickname })
            });

            latestGameEvent = { type: 'INFO', message: 'Waiting for game to start...' };
        },

        onStompError: (frame) => {
            console.error('Broker reported error: ' + frame.headers['message']);
            console.error('Additional details: ' + frame.body);
            latestGameEvent = { type: 'ERROR', message: 'Connection Error' };
        }
    });

    client.activate();
}
