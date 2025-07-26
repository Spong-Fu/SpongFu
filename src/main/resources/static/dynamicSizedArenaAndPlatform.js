const dynamicSizedArenaAndPlatform = (aspectRatioWidth, aspectRatioHeight) =>  {
 //canvas takes up 80% of screen and sets height to desired aspect ratio.
	const arena = { w: window.innerWidth * .8, h: (window.innerWidth * .8) * (aspectRatioHeight/aspectRatioWidth) }; 
	const platformWidth = arena.w * .5
	const platformHeight = platformWidth * .35;
	const platform = { x: arena.w * .25, y: arena.h - platformHeight, w: platformWidth, h: platformHeight}

	return [arena, platform];
};

export default dynamicSizedArenaAndPlatform;
