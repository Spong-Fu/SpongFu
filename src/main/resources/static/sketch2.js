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
		let s = random(80,100);
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
const dynamicSizedArenaAndPlatform = (aspectRatioWidth, aspectRatioHeight) => {
	const arena = { w: window.innerWidth * .8, h: (window.innerWidth * .8) * (aspectRatioHeight/aspectRatioWidth) };
	const platformWidth = arena.w * .5;
	const platformHeight = platformWidth * .35;
	const platform = {x: arena.w * .25, y: arena.h - platformHeight, w: platformWidth, h: platformHeight};

	return [arena, platform];
}

let players = []
let pointer
const numPlayers = 1
// style out these 2 please
const [arena, platform] = dynamicSizedArenaAndPlatform(2,1);
let gameOver = false

function setup() {
	let canvas = createCanvas(arena.w, arena.h)
	canvas.parent("game-container")
	for (let i = 0; i < numPlayers; i++) {
		players.push(
			new Player(
				random(width * 0.2, width * 0.8),
				random(height * 0.2, height * 0.8),
				color(random(50, 255), random(50, 255), random(50, 255))
			)
		)
	}

	pointer = new Spinner()
}

function draw() {
	//background(34)
	drawBackground();

	//Draw platform
	fill("gray")
	stroke(200)
	rect(platform.x, platform.y, platform.w, platform.h)

	// Draw arena border
	noFill()
	stroke(200)
	rect(0, 0, width, height)

	// Update and draw players
	players.forEach((p) => p.update())
	players.forEach((p) => p.draw())

	// Draw spinning pointer
	pointer.update()
	pointer.draw(players[0].pos)

	//Loose condition
	if (!gameOver && players[0].pos.y - players[0].r > height) {
		noLoop()
		gameOver = true
	}

	//Draw game over
	if (gameOver) {
		push()
		textAlign(CENTER, TOP)
		textSize(72)
		fill("red")
		noStroke()
		text("Game Over", width / 2, 20)
		pop()
		return
	}
}

function mousePressed() {
	if (mouseButton === LEFT) {
		players.forEach((p) => p.launch(pointer.angle))
	}
}

class Player {
	constructor(x, y, col) {
		this.pos = createVector(x, y)
		this.vel = createVector()
		this.r = 20
		this.col = col
	}

	update() {
		// basic physics: apply velocity, friction, gravity optional
		this.pos.add(this.vel)
		this.vel.mult(0.98)

		// bounce walls
		if (this.pos.x < this.r || this.pos.x > width - this.r) {
			this.vel.x *= -1
		}
		if (this.pos.y < this.r) {
			this.vel.y *= -1
		}

		if (
			this.pos.y + this.r > platform.y &&
			this.pos.y - this.vel.y + this.r <= platform.y && // was above last frame
			this.pos.x > platform.x &&
			this.pos.x < platform.x + platform.w
		) {
			// snap to platform surface and reverse Y
			this.pos.y = platform.y - this.r
			this.vel.y *= -1
		}
	}

	draw() {
		fill(this.col)
		noStroke()
		ellipse(this.pos.x, this.pos.y, this.r * 2)
	}

	launch(angle) {
		let force = p5.Vector.fromAngle(angle).mult(10).mult(-1)
		this.vel.add(force)
	}
}

class Spinner {
	constructor() {
		this.angle = 0
		this.speed = 0.1
		this.len = 100
		this.center = createVector(width / 2, height / 2)
	}

	update() {
		this.angle += this.speed
	}

	draw(atpos) {
		push()
		translate(atpos.x, atpos.y)
		rotate(this.angle)
		stroke(255)
		strokeWeight(4)
		line(0, 0, this.len, 0)
		pop()
	}
}
