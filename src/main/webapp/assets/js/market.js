document.addEventListener("submit", (event) => {
  const form = event.target;
  const message = event.submitter?.dataset.confirmButton || form.dataset.confirm;
  if (message && !window.confirm(message)) {
    event.preventDefault();
    return;
  }
  const button = event.submitter;
  if (button?.hasAttribute("data-submit-once")) {
    if (form.dataset.submitting) {
      event.preventDefault();
      return;
    }
    form.dataset.submitting = "true";
    button.dataset.originalText = button.textContent;
    button.textContent = "Saving…";
    // Keep the submitter enabled so its name/value remains part of the request.
    button.setAttribute("aria-disabled", "true");
  }
});
document.querySelectorAll("[data-preview]").forEach((input) => {
  input.addEventListener("change", () => {
    const image = document.getElementById(input.dataset.preview);
    input.setCustomValidity("");
    if (!input.files[0]) return;
    if (input.files[0].size > 5 * 1024 * 1024) {
      input.setCustomValidity("Choose an image under 5 MB.");
      input.reportValidity();
      return;
    }
    input.setCustomValidity("");
    if (image.dataset.objectUrl) URL.revokeObjectURL(image.dataset.objectUrl);
    image.dataset.objectUrl = URL.createObjectURL(input.files[0]);
    image.src = image.dataset.objectUrl;
  });
});
window.addEventListener("pageshow", () => {
  document.querySelectorAll("[data-submitting]").forEach((form) => {
    delete form.dataset.submitting;
  });
  document.querySelectorAll("[data-submit-once]").forEach((button) => {
    button.removeAttribute("aria-disabled");
    if (button.dataset.originalText) button.textContent = button.dataset.originalText;
  });
});
