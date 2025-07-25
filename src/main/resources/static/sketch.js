let players = []
let pointer
const numPlayers = 1
// style out these 2 please
const arena = { x: 0, y: 0, w: 1300, h: 700 }
const platform = { x: 350, y: 500, w: 600, h: 200 }
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
	background(34)

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
