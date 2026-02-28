/**
 * TODO(@mike priority:HIGH category:ui issue:PROJ-102): Implement the new Dark Mode toggle switch
 */
function toggleTheme() {
  // TODO(priority:LOW due:2028-04-15 platform:web): Add aria-labels to the toggle button
  const isDark = document.body.classList.contains("dark");

  // FIXME(@alex priority:CRITICAL category:bug): This flashes white on initial page load
  localStorage.setItem("theme", isDark ? "light" : "dark");
}
