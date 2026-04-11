<template>
  <div class="placeholder-page">
    <header class="page-header">
      <button class="back-btn" @click="$router.back()">← 返回上一步</button>
      <h1>場地日曆 (開發中)</h1>
      <p>這裡是下一階段要實作 VenueCalendar 的地方</p>
    </header>

    <div class="empty-state">
      <div class="icon">🚧</div>
      <h2>施工中</h2>
      <p>FullCalendar 日曆套件即將在這裡登場！</p>
    </div>
  </div>
</template>

<script setup>
// 這裡目前只需要一個空殼，用來確保路由可以正常跳轉
</script>

<style lang="scss" scoped>
.placeholder-page {
  text-align: center;

  .page-header {
    margin-bottom: 2.5rem;
    h1 {
      color: #2d3436;
      margin-bottom: 0.5rem;
    }
    p {
      color: #636e72;
    }
    .back-btn {
      background: none;
      border: none;
      color: #0984e3;
      cursor: pointer;
      margin-bottom: 1rem;
      &:hover {
        text-decoration: underline;
      }
    }
  }

  .empty-state {
    background: white;
    padding: 4rem 2rem;
    border-radius: 12px;
    border: 1px dashed #b2bec3;

    .icon {
      font-size: 3rem;
      margin-bottom: 1rem;
    }

    h2 {
      color: #2d3436;
      margin-bottom: 0.5rem;
    }

    p {
      color: #636e72;
    }
  }
}
</style>
