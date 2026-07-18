<template>
  <section class="hero">
    <div class="copy">
      <p class="brand-line">TuGuLu</p>
      <h1>Warehouse clarity for every parcel.</h1>
      <p class="lead">Track inbound packages, explore our services, and move freight with confidence.</p>
      <div class="actions">
        <router-link class="primary" to="/track">Track now</router-link>
        <router-link class="ghost" to="/company">About us</router-link>
      </div>
    </div>
    <div class="visual" aria-hidden="true">
      <div class="panel"></div>
      <div class="panel delay"></div>
    </div>
  </section>
</template>

<style scoped>
.hero {
  min-height: calc(100vh - 140px);
  display: grid;
  grid-template-columns: 1.1fr .9fr;
  gap: 40px;
  align-items: center;
  animation: rise .7s ease both;
}
.brand-line {
  font-family: var(--font-display);
  font-size: clamp(2.4rem, 5vw, 4rem);
  margin: 0 0 8px;
  color: var(--moss);
}
h1 {
  margin: 0;
  font-family: var(--font-display);
  font-size: clamp(1.6rem, 3vw, 2.4rem);
  font-weight: 600;
  max-width: 14ch;
  line-height: 1.15;
}
.lead {
  margin: 16px 0 28px;
  color: var(--muted);
  max-width: 38ch;
  font-size: 1.05rem;
}
.actions { display: flex; gap: 12px; flex-wrap: wrap; }
.primary, .ghost {
  padding: 12px 18px;
  border-radius: 999px;
  font-weight: 700;
}
.primary { background: var(--moss); color: #fff; }
.ghost { border: 1px solid rgba(20,35,31,.2); }
.visual {
  position: relative;
  min-height: 420px;
}
.panel {
  position: absolute;
  inset: 8% 8% 8% 18%;
  border-radius: 28px;
  background:
    linear-gradient(135deg, rgba(31,77,64,.92), rgba(20,35,31,.85)),
    url('https://images.unsplash.com/photo-1586528116311-ad8dd3c8310d?auto=format&fit=crop&w=1400&q=80') center/cover;
  box-shadow: 0 30px 80px rgba(20,35,31,.25);
  animation: float 6s ease-in-out infinite;
}
.panel.delay {
  inset: 22% 0 0 40%;
  opacity: .35;
  filter: blur(0px);
  background: linear-gradient(160deg, var(--gold), transparent 70%);
  animation-delay: .8s;
}
@keyframes rise {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: none; }
}
@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}
@media (max-width: 900px) {
  .hero { grid-template-columns: 1fr; min-height: auto; padding-top: 24px; }
  .visual { min-height: 280px; }
}
</style>
