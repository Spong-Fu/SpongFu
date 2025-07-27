// --- Networking & State Management ---
var client;
// We no longer need mySessionId, as we will use myNickname to identify our player.
var gameId = null;
var isJoining = false;
var myNickname = null;

var latestGameState = null;
var latestGameEvent = null;

// --- p5.js Sketch ---
const arena = { w: 800, h: 400 };
const platform = { x: 100, y: 300, w: 600, h: 90 };

function setup() {
    let canvas = createCanvas(arena.w, arena.h);
    canvas.parent("game-container");
}

function initializeLobby() {
    const joinButton = document.getElementById('join-button');
    const nicknameInput = document.getElementById('nickname');
    const lobby = document.getElementById('lobby');
    const gameContainer = document.getElementById('game-container');

    if (joinButton && nicknameInput && lobby && gameContainer) {
        joinButton.onclick = () => {
            const nickname = nicknameInput.value.trim();
            if (nickname) {
                myNickname = nickname; // Store the nickname
                isJoining = true;
                lobby.style.display = 'none';
                gameContainer.style.display = 'block';
                initNetworking(nickname);
            } else {
                alert('Please enter a nickname!');
            }
        };
    } else {
        console.error("Lobby elements could not be found.");
    }
}

window.addEventListener('DOMContentLoaded', initializeLobby);


function draw() {
    background(34);

    // translate(width / 2, height / 2);

    if (!isJoining && gameId) {
        fill('gray');
        stroke(200);
        rect(platform.x, platform.y, platform.w, platform.h);
        noFill();
        stroke(200);
        rect(0, 0, width - 1, height - 1);
    }
    
    let status = '';
    if (isJoining) {
        status = 'Connecting...';
    } else if (!gameId) {
        status = 'In lobby, waiting for other players...';
    } else if (!latestGameState) {
        status = `Game found! Waiting for round to start...`;
    }

    if (status) {
        drawStatusText(status);
        return;
    }

    if (latestGameState && latestGameState.players) {
        console.log("Drawing players:", latestGameState.players); 
        latestGameState.players.forEach(playerDto => {
            drawPlayer(playerDto);
        });
    }

    if (latestGameEvent && latestGameEvent.type === 'ROUND_WINNER') {
        drawStatusText(latestGameEvent.message);
    }
}

// This function draws a single player based on the PlayerStateDto from the server.
function drawPlayer(playerDto) {
    console.log("Attempting to draw player:", playerDto);

    const { x, y, size, nickname, angle } = playerDto;

    // const screenX = x + width / 2;
    // const screenY = y + height / 2;
    
    // push();
    fill('#FFC0CB'); 
    noStroke();
    ellipse(x, y, size * 2);
    fill(255);
    textAlign(CENTER, BOTTOM);
    textSize(14);
    text(nickname, x, y - size - 5);

    if (nickname === myNickname) {
        push();
        translate(x, y);
        rotate(angle);
        stroke(255);
        strokeWeight(3);
        line(0, 0, size + 20, 0); 
        pop();
    }
    // pop();
}

function drawStatusText(textToShow) {
    push();
    textAlign(CENTER, CENTER);
    textSize(32);
    fill("white");
    noStroke();
    text(textToShow, width / 2, height / 2);
    pop();
}

function keyPressed() {
    if (key === ' ' && client && client.active && gameId) {
        // Action logic will be uncommented when the backend action endpoint is ready.
    }
}

function initNetworking(nickname) {
    client = new StompJs.Client({
        brokerURL: 'ws://localhost:8080/ws', 
        reconnectDelay: 5000,
        debug: msg => console.log('[STOMP]', msg),
        onConnect: (frame) => {
            isJoining = false;
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

function onPrivateMessage(message) {
    const data = JSON.parse(message.body);
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
