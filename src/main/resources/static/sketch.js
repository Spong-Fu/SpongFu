var client;
var mySessionId = null; 
var gameId = null; 
var isJoining = false;

var latestGameState = null;
var latestGameEvent = null;

const arena = { w: 1200, h: 700 };

function setup() {
    let canvas = createCanvas(arena.w, arena.h);
    canvas.parent("game-container");
    
    const joinButton = document.getElementById('join-button');
    const nicknameInput = document.getElementById('nickname');
    const lobby = document.getElementById('lobby');
    const gameContainer = document.getElementById('game-container');

    if (joinButton && nicknameInput && lobby && gameContainer) {
        joinButton.onclick = () => {
            const nickname = nicknameInput.value.trim();
            if (nickname) {
                isJoining = true;
                lobby.style.display = 'none';
                gameContainer.style.display = 'block';
                initNetworking(nickname);
            } else {
                alert('Please enter a nickname!');
            }
        };
    } else {
        console.error("Lobby elements not found! Ensure HTML is correct.");
    }
}

function draw() {
    background(34);

    let status = '';
    if (isJoining) {
        status = 'Connecting...';
    } else if (!gameId) {
        status = 'In lobby, waiting for game to start...';
    } else if (!latestGameState) {
        status = `Game ${gameId} starting... Waiting for state.`;
    }

    if (status) {
        drawStatusText(status);
        return;
    }

    // --- Draw the Game World ---
    push();
    translate(width / 2, height / 2);

    const arenaRadius = latestGameState.currentArenaRadius || 300;
    fill(100);
    noStroke();
    ellipse(0, 0, arenaRadius * 2);

    Object.values(latestGameState.players).forEach(player => {
        if (!player.isEliminated) {
            drawPlayer(player);
        }
    });
    pop();

    if (latestGameEvent && latestGameEvent.type === 'ROUND_WINNER') {
        drawStatusText(latestGameEvent.message);
    }
}

function drawPlayer(player) {
    const { x, y, mass, nickname, sessionId, angle, color } = player;
    
    fill(color || '#fff');
    noStroke();
    ellipse(x, y, mass * 2);

    fill(255);
    textAlign(CENTER, BOTTOM);
    textSize(14);
    text(nickname, x, y - mass - 5);

    if (sessionId === mySessionId) {
        push();
        translate(x, y);
        rotate(angle);
        stroke(255);
        strokeWeight(3);
        line(0, 0, mass + 20, 0);
        pop();
    }
}

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

function keyPressed() {
    if (key === ' ' && client && client.active && gameId) {
        console.log(`Sending 'expel' action for game ${gameId}`);
        
        // this is for MessageMapping("/game/{gameId}/action") part
        /*
        client.publish({
            destination: `/app/game/${gameId}/action`,
            body: JSON.stringify({ action: 'EXPEL' }) // Match the Java Enum
        });
        */
    }
}

function initNetworking(nickname) {
    client = new StompJs.Client({
        brokerURL: 'ws://localhost:8080/ws', 
        reconnectDelay: 5000,
        debug: msg => console.log('[STOMP]', msg),
        onConnect: (frame) => {
            console.log('STOMP client connected.');
            isJoining = false;
            mySessionId = frame.headers['user-name'];
            console.log('My session ID is:', mySessionId);

            client.subscribe('/user/queue/private', onPrivateMessage);

            client.publish({
                destination: '/app/game.find',
                body: JSON.stringify({ nickname: nickname }) 
            });
        },
        onStompError: (frame) => {
            console.error('Broker reported error: ' + frame.headers['message']);
            isJoining = false;
        }
    });
    client.activate();
}

/**
 * Handles messages received on the private user queue.
 * The most important message is the one containing our gameId.
 * @param {object} message - The STOMP message object.
 */
function onPrivateMessage(message) {
    const data = JSON.parse(message.body);
    console.log('[PRIVATE MSG]', data);

    if (data.gameId) {
        gameId = data.gameId;
        console.log(`Joined game! Game ID: ${gameId}`);

        client.subscribe(`/topic/game.state/${gameId}`, (message) => {
            latestGameState = JSON.parse(message.body);
        });

        client.subscribe(`/topic/game.events/${gameId}`, (message) => {
            latestGameEvent = JSON.parse(message.body);
        });
    }
}
