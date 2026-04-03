const form = document.getElementById("support-form");
const submitButton = document.getElementById("submit-btn");
const feedback = document.getElementById("feedback");

function showFeedback(message, type) {
  feedback.textContent = message;
  feedback.className = `feedback ${type}`;
}

function resetFeedback() {
  feedback.textContent = "";
  feedback.className = "feedback";
}

form.addEventListener("submit", async (event) => {
  event.preventDefault();
  resetFeedback();

  const name = document.getElementById("name").value.trim();
  const contact = document.getElementById("contact").value.trim();
  const originalMessage = document.getElementById("message").value.trim();

  if (!name || !contact || !originalMessage) {
    showFeedback("Please complete all required fields before submitting.", "error");
    return;
  }

  const payload = {
    name,
    contact,
    originalMessage
  };

  const defaultButtonText = "Submit Request";
  submitButton.disabled = true;
  submitButton.textContent = "Submitting...";

  try {
    const response = await fetch("/api/requests", {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify(payload)
    });

    if (!response.ok) {
      throw new Error(`Request failed with status ${response.status}`);
    }

    showFeedback("Request received. Our team will contact you shortly.", "success");
    form.reset();
  } catch (error) {
    showFeedback(
      "We could not submit your request right now. Please check your internet or try again in a moment.",
      "error"
    );
  } finally {
    submitButton.disabled = false;
    submitButton.textContent = defaultButtonText;
  }
});
