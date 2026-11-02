const money = value => new Intl.NumberFormat("en-IN", {
  style: "currency", currency: "INR", minimumFractionDigits: 2
}).format(Number(value || 0));

function showNotice(message, type = "success") {
  const notice = document.getElementById("notice");
  notice.textContent = message;
  notice.className = `notice ${type}`;
  notice.scrollIntoView({ behavior: "smooth", block: "nearest" });
}

async function api(path, data) {
  const options = data ? {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
    body: new URLSearchParams(data)
  } : {};
  const response = await fetch(path, options);
  const result = await response.json();
  if (!response.ok) throw new Error(result.error || "Request failed.");
  return result;
}

async function refreshData() {
  try {
    const [accountData, transactionData] = await Promise.all([
      api("/api/accounts"), api("/api/transactions")
    ]);
    const accounts = accountData.accounts || [];
    document.getElementById("accountCount").textContent = accounts.length;
    document.getElementById("totalBalance").textContent =
      money(accounts.reduce((sum, a) => sum + Number(a.balance), 0));
    document.getElementById("savingsCount").textContent =
      accounts.filter(a => a.type === "SAVINGS").length;

    const accountsBody = document.getElementById("accountsBody");
    accountsBody.innerHTML = accounts.length ? accounts.map(a => `
      <tr>
        <td><strong>${a.number}</strong></td>
        <td>${safe(a.name)}</td>
        <td><span class="pill">${a.type}</span></td>
        <td>${money(a.balance)}</td>
        <td>${a.type === "SAVINGS" ? money(a.interest) : "—"}</td>
      </tr>`).join("") : `<tr><td class="empty" colspan="5">No accounts yet.</td></tr>`;

    const transactions = (transactionData.transactions || []).slice().reverse();
    const transactionBody = document.getElementById("transactionsBody");
    transactionBody.innerHTML = transactions.length ? transactions.map(t => `
      <tr>
        <td>${safe(t.time)}</td><td>${t.account}</td><td><span class="pill">${safe(t.type)}</span></td>
        <td>${safe(t.details)}</td><td>${money(t.amount)}</td><td>${money(t.balance)}</td>
      </tr>`).join("") : `<tr><td class="empty" colspan="6">Transactions will appear here.</td></tr>`;
  } catch (error) {
    showNotice("Could not load bank data. Is the Java server running?", "error");
  }
}

function safe(value) {
  return String(value ?? "").replace(/[&<>"']/g, ch => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
  }[ch]));
}

document.querySelectorAll("form[data-action]").forEach(form => {
  form.addEventListener("submit", async event => {
    event.preventDefault();
    const values = Object.fromEntries(new FormData(form).entries());
    values.action = form.dataset.action;
    try {
      const result = await api("/", values);
      showNotice(result.message || "Action completed successfully.", "success");
      form.reset();
      await refreshData();
    } catch (error) {
      showNotice(error.message, "error");
    }
  });
});

document.getElementById("refreshBtn").addEventListener("click", refreshData);
refreshData();
