//Rendering Functions
const drawTriangleChunk = (x, y, size) => {
	beginShape();
	for (let i=0; i<3; i++) {
		let angle = random(TWO_PI);
		let radius = random(size /2, size);
		let vx = x + radius * cos(angle);
		let vy = y + radius * sin(angle);
		vertex(vx, vy);
	}
	endShape(CLOSE);
}


const drawHSBBackground = () => {
	push();
	colorMode(HSB, 360, 100, 100, 255);
	noStroke();
	const density = 100; 
	for (let x=0; x<width+density; x+=density) {
		for (let y =0; y<height + density; y+=density) {
		let h = random(20, 40);
		let s = random(80, 100);
		let b = random(70, 100);
		fill(h, s, b, 150);
		drawTriangleChunk(x, y, density);

	}
 }
	pop();
}


const drawBackground = () => {
	push();
	noStroke();
	const density = 100; //triangle size
	for (let x=0; x<width+density; x+=density) {
		for (let y=0; y<height+density; y += density) {
			let r = random(30, 60);
			let g = random(25, 40);
			let b = random(20, 30);
			fill(r, g, b, 150);
			drawTriangleChunk(x, y, density);
		}
	}
	pop();
}


//canvas takes up 80% of screen and sets height to desired aspect ratio.
const dynamicSizedArena = (aspectRatioWidth, aspectRatioHeight) => {
	const arena = { w: window.innerWidth * .8, h: (window.innerWidth * .8) * (aspectRatioHeight/aspectRatioWidth) };

	return arena;
}


// --- Networking & State Management ---
var client;
// We no longer need mySessionId, as we will use myNickname to identify our player.
var gameId = null;
var isJoining = false;
var myNickname = null;

var latestGameState = null;
var latestGameEvent = null;

// --- p5.js Sketch ---

function setup() {
    // Create a larger square canvas to accommodate the circular arena
    // The initial arena radius is 500, so we need at least 1000x1000 canvas
		const arena = dynamicSizedArena(2,1);
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
								gameContainer.style.display = "block";
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

    if (!isJoining && gameId && latestGameState) {
        // Draw circular arena using the radius from the server
        let centerX = width / 2;
        let centerY = height / 2;
        let arenaRadius = latestGameState.arenaRadius;

        // Draw arena boundary circle
        noFill();
        stroke(200);
        strokeWeight(2);
        ellipse(centerX, centerY, arenaRadius * 2);

        // Optional: Add a subtle fill to show the playable area
        fill(50, 50, 50, 50); // Dark gray with transparency
        ellipse(centerX, centerY, arenaRadius * 2);
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


function drawPlayer(playerDto) {
    console.log("Attempting to draw player:", playerDto);

    const { x, y, size, nickname, angle } = playerDto;

    // Convert from game coordinates (centered at 0,0) to screen coordinates
    const screenX = x + width / 2;
    const screenY = y + height / 2;

    fill('#FFC0CB');
    noStroke();
    ellipse(screenX, screenY, size * 2);

    fill(255);
    textAlign(CENTER, BOTTOM);
    textSize(14);
    text(nickname, screenX, screenY - size - 5);

    if (nickname === myNickname) {
        push();
        translate(screenX, screenY);
        rotate(angle);
        stroke(255);
        strokeWeight(3);
        line(0, 0, size + 20, 0);
        pop();
    }
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
        client.publish({
            destination: `/app/game.action/${gameId}`,
            body: JSON.stringify({ action: 'EXPEL' })
        });
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
