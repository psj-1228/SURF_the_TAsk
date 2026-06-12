(function () {
    const form = document.querySelector("[data-auth-form]");

    if (!form) {
        return;
    }

    const message = form.querySelector("[data-form-message]");
    const submitButton = form.querySelector("button[type='submit']");
    const passwordFindLink = document.querySelector("[data-password-find]");
    const authToast = document.querySelector("[data-auth-toast]");
    const mode = form.dataset.authForm;
    let authToastTimer = null;

    if (passwordFindLink && authToast) {
        passwordFindLink.addEventListener("click", function (event) {
            event.preventDefault();
            showAuthToast("개발 진행 중입니다.");
        });
    }

    form.addEventListener("submit", async function (event) {
        event.preventDefault();
        clearMessage();

        if (!form.reportValidity()) {
            return;
        }

        const payload = Object.fromEntries(new FormData(form).entries());
        setBusy(true);

        try {
            const response = await fetch(mode === "register" ? "/api/auth/register" : "/api/auth/login", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(payload)
            });
            const body = await readJson(response);

            if (!response.ok) {
                showMessage(formatError(body), "error");
                return;
            }

            if (mode === "register") {
                showMessage("Account created. Please log in.", "success");
                window.setTimeout(function () {
                    window.location.href = "/login?registered=true";
                }, 800);
                return;
            }

            localStorage.setItem("surfUser", JSON.stringify({
                userId: body.userId,
                loginId: body.loginId,
                name: body.name,
                token: body.token
            }));
            showMessage("Logged in as " + body.name + ".", "success");
            window.setTimeout(function () {
                window.location.href = "/dashboard";
            }, 500);
        } catch (error) {
            showMessage("Could not connect to the server. Please try again.", "error");
        } finally {
            setBusy(false);
        }
    });

    if (mode === "login" && new URLSearchParams(window.location.search).get("registered") === "true") {
        showMessage("Account created. Log in with your new credentials.", "success");
    }

    function readJson(response) {
        return response.text().then(function (text) {
            return text ? JSON.parse(text) : {};
        });
    }

    function formatError(body) {
        if (Array.isArray(body.details) && body.details.length > 0) {
            return body.details.join(" / ");
        }
        return body.message || "Request failed.";
    }

    function showMessage(text, type) {
        message.textContent = text;
        message.className = "form-message " + type;
    }

    function clearMessage() {
        message.textContent = "";
        message.className = "form-message";
    }

    function showAuthToast(text) {
        window.clearTimeout(authToastTimer);
        authToast.textContent = text;
        authToast.hidden = false;
        authToast.classList.add("is-visible");
        authToastTimer = window.setTimeout(function () {
            authToast.classList.remove("is-visible");
            authToast.hidden = true;
        }, 2200);
    }

    function setBusy(isBusy) {
        submitButton.disabled = isBusy;
        submitButton.textContent = isBusy ? "Processing..." : (mode === "register" ? "Create account" : "Log in");
    }
})();
