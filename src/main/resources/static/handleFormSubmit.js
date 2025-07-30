document.querySelector(".name-submission-form").addEventListener("submit", function(event) {
		event.preventDefault();
		
		const nickname = document.getElementById("nickName").value.trim();

		if (nickname) {
		window.location.href = `${window.location.origin}/?nickName=${encodeURIComponent(nickname)}`;
		}
	});
