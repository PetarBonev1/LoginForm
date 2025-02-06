document.getElementById("loginForm").addEventListener("submit", function(event) {
    event.preventDefault();
    const email = document.getElementById("email").value;
    const password = document.getElementById("password").value;

    fetch("/login", {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: `email=${encodeURIComponent(email)}&password=${encodeURIComponent(password)}`
    })
        .then(response => {
            if (response.ok) {
                document.getElementById("loginForm").classList.add("hidden");
                document.getElementById("welcome").classList.remove("hidden");
                document.getElementById("logoutBtn").classList.remove("hidden");
            } else {
                alert("Invalid Credentials!");
            }
        });
});

document.getElementById("logoutBtn").addEventListener("click", function() {
    fetch("/logout", { method: "POST" })
        .then(response => {
            if (response.ok) {
                document.getElementById("loginForm").classList.remove("hidden");
                document.getElementById("welcome").classList.add("hidden");
                document.getElementById("logoutBtn").classList.add("hidden");
            }
        });
});
