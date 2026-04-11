<template>
  <nav class="navbar">
    <div class="nav-brand" @click="$router.push('/')">
      國立中央大學學務處<span>場地租借系統</span>
    </div>
    <div class="nav-user">
      <div class="avatar-circle">
        <User :size="20" stroke-width="2.5" />
      </div>
    </div>
  </nav>
</template>

<script setup>
import { User } from "lucide-vue-next";
</script>

<style lang="scss" scoped>
.navbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 2rem;
  background: white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  position: sticky;
  top: 0;
  z-index: 100;

  .nav-brand {
    font-weight: 800;
    font-size: 1.25rem;
    letter-spacing: 1px;
    cursor: pointer;
    color: #2d3436;
    span {
      color: #0984e3;
    }
  }

  .avatar-circle {
    width: 38px;
    height: 38px;
    background: #f1f2f6;
    border-radius: 50%;
    display: flex;
    justify-content: center;
    align-items: center;
    color: #636e72;
    cursor: pointer;
    transition: background 0.2s;
    &:hover {
      background: #dfe6e9;
    }
  }
}
</style>
