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


const drawBackground = () => {
	push();
	noStroke();
	const density = 100; 
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

var client;
var gameId = null;
var isJoining = false;
var myNickname = null;
var winnerInfo = null;

var latestGameState = null;
var latestGameEvent = null;

let spongeImg;
let googleyEyes;

var floatingTexts = [];

class FloatingText {
    constructor(message, x, y, type) {
        this.message = message;
        this.x = x;
        this.y = y;
        this.type = type;
        this.lifespan = 255; 
    }

    update() {
        if (this.type === "remote") {
            this.y -= 1; 
        }
        this.lifespan -= 2; 
    }

    draw() {
        push(); 
        textAlign(CENTER, CENTER);

        if (this.type === 'local') {
            textSize(64);
            fill(255, 0, 0, this.lifespan);
            stroke(0, this.lifespan);
            strokeWeight(4);
            text(this.message, this.x, this.y);
        } else {
            textSize(24);
            fill(255, 255, 255, this.lifespan); 
            stroke(0, this.lifespan);
            strokeWeight(2);
            text(this.message, this.x, this.y);
        }
        pop(); 
    }

    isFinished() {
        return this.lifespan < 0;
    }
}

function preload() {
	spongeImg = loadImage("./assets/sponge-svgrepo-com.svg");
	googleyEyes = loadImage("./assets/googley-eyes-1.svg");
}

function setup() {
    let canvasSize = 1200; 
    let canvas = createCanvas(canvasSize, canvasSize);
    canvas.parent("game-container");
}

function initializeLobby() {
    const nicknameInput = new URLSearchParams(window.location.search).get("nickName");
		myNickname = nicknameInput.trim();
		isJoining = true;
    const gameContainer = document.getElementById('game-container');
    gameContainer.style.display = 'block';
    initNetworking(myNickname);
}

window.addEventListener('DOMContentLoaded', initializeLobby);


function draw() {
		drawBackground();

    if (!isJoining && gameId && latestGameState) {
        let centerX = width / 2;
        let centerY = height / 2;
        let arenaRadius = latestGameState.arenaRadius * 0.95; // Make arena 0.9x the size from BE

        noFill();
        stroke(200);
        strokeWeight(2);
        ellipse(centerX, centerY, arenaRadius * 2);

        fill(50, 50, 50, 50); 
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
        // console.log("Drawing players:", latestGameState.players);
        latestGameState.players.forEach(playerDto => {
            drawPlayer(playerDto);
        });
    }

    for (let i = floatingTexts.length - 1; i >= 0; i--) {
        const ft = floatingTexts[i];
        ft.update();
        ft.draw();
        if (ft.isFinished()) {
            floatingTexts.splice(i, 1);
        }
    }

    if (winnerInfo) {
        drawStatusText(winnerInfo.message );
    }
}


function drawPlayer(playerDto) {
    // console.log("Attempting to draw player:", playerDto);

    const { x, y, size, nickname, angle } = playerDto;

    const screenX = x + width / 2;
    const screenY = y + height / 2;

		push();
		imageMode(CENTER);
		const spongeSize = size * 2;
		const eyeSize = spongeSize * .4;
		image(spongeImg, screenX, screenY, spongeSize, spongeSize);
		image(googleyEyes, screenX, screenY - spongeSize *.1, eyeSize, eyeSize)
		pop();

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
        // console.log(`Joined game! Game ID: ${gameId}`);

        client.subscribe(`/topic/game.state/${gameId}`, (message) => {
            latestGameState = JSON.parse(message.body);

            // if (latestGameState.players) {
            //     latestGameState.players.forEach(p => {
            //         if (!playerStatus[p.nickname]) {
            //             playerStatus[p.nickname] = { isAlive: true };
            //         }
            //     });
            // }
        });

        client.subscribe(`/topic/game.events/${gameId}`, (message) => {
            latestGameEvent = JSON.parse(message.body);
            console.log("latestGameEvent: ",latestGameEvent);
            

            if (latestGameEvent.eventType === 'PLAYER_ELIMINATED') {
                const eliminatedPlayerNickname = latestGameEvent.message;

                if (eliminatedPlayerNickname === myNickname) {
                    floatingTexts.push(new FloatingText("You were eliminated!", width / 2, height / 2, 'local'));
                } else {
                    const player = latestGameState.players.find(p => p.nickname === eliminatedPlayerNickname);
                    if (player) {
                    const screenX = parseFloat(player.x) + width / 2;
                    const screenY = parseFloat(player.y) + height / 2;
                    floatingTexts.push(new FloatingText(`${eliminatedPlayerNickname} eliminated!`, screenX, screenY, 'remote'));
                }
            }
        } else if (latestGameEvent.eventType === 'ROUND_WINNER') {
            const winnerNickname = latestGameEvent.message;
            if (winnerNickname === myNickname) {
                winnerInfo = { message: "Congratulations!! You Won!" };
            } else {
                winnerInfo = { message: `${winnerNickname} wins!, better luck next time` };
            }
        }
    });
}
}
