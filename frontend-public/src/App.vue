<template>
  <div class="shell">
    <header class="top">
      <router-link to="/" class="brand">TuGuLu</router-link>
      <nav>
        <router-link to="/company">Company</router-link>
        <router-link to="/service">Service</router-link>
        <router-link to="/product">Product</router-link>
        <router-link to="/track" class="cta">Track Parcel</router-link>
      </nav>
      <DualClock />
    </header>
    <main>
      <router-view />
    </main>
    <footer>
      <span>TuGuLu Warehouse & Logistics</span>
    </footer>
  </div>
</template>

<script setup>
import DualClock from './components/DualClock.vue'
</script>

<style scoped>
.shell { min-height: 100vh; display: flex; flex-direction: column; }
.top {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  gap: 16px;
  padding: 18px 6vw;
}
.brand {
  font-family: var(--font-display);
  font-size: 2rem;
  font-weight: 700;
  color: var(--moss);
  letter-spacing: .02em;
}
nav {
  display: flex;
  gap: 18px;
  align-items: center;
  justify-content: center;
}
nav a {
  color: var(--muted);
  font-weight: 600;
}
nav a.router-link-active { color: var(--ink); }
.cta {
  background: var(--moss);
  color: #fff !important;
  padding: 8px 14px;
  border-radius: 999px;
}
main { flex: 1; padding: 0 6vw 48px; }
footer {
  padding: 20px 6vw 28px;
  color: var(--muted);
  border-top: 1px solid rgba(20,35,31,.08);
}
@media (max-width: 860px) {
  .top { grid-template-columns: 1fr; justify-items: start; }
  nav { justify-content: flex-start; flex-wrap: wrap; }
}
</style>
