<template>
  <label class="language-selector">
    <span class="language-label">{{ t("nav.language") }}</span>
    <select
      :value="locale"
      :aria-label="t('nav.languageSelectorAria')"
      @change="handleLocaleChange"
    >
      <option value="zh-TW">{{ t("nav.traditionalChinese") }}</option>
      <option value="en-US">{{ t("nav.english") }}</option>
    </select>
  </label>
</template>

<script setup>
import { useI18n } from "vue-i18n";
import { setLocale } from "@/i18n";

const { locale, t } = useI18n();

const handleLocaleChange = (event) => {
  setLocale(event.target.value);
};
</script>

<style lang="scss" scoped>
.language-selector {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  color: #19355a;
  font-size: var(--text-sm);
  font-weight: 700;
}

.language-selector select {
  min-height: 2.35rem;
  padding: 0.35rem 1.9rem 0.35rem 0.65rem;
  border: 1px solid rgba(36, 63, 107, 0.2);
  border-radius: var(--radius-sm);
  background: #f8fbff;
  color: #19355a;
  cursor: pointer;
}

@media (max-width: 640px) {
  .language-label {
    position: absolute;
    width: 1px;
    height: 1px;
    padding: 0;
    margin: -1px;
    overflow: hidden;
    clip: rect(0, 0, 0, 0);
    white-space: nowrap;
    border: 0;
  }

  .language-selector select {
    max-width: 7.25rem;
  }
}
</style>
