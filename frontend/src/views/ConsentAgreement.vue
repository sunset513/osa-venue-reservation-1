<template>
  <div class="consent-page page-enter">
    <header class="page-header consent-header">
      <p class="hero-eyebrow">Venue Use Consent</p>
      <h1>場地借用同意書</h1>
      <p>請確認借用規範、設備責任與個人資料使用聲明後，再進入場地租借系統。</p>
    </header>

    <section class="agreement-panel card" aria-labelledby="agreement-title">
      <div class="agreement-summary">
        <div class="summary-icon" aria-hidden="true">
          <ClipboardCheck :size="28" />
        </div>
        <div>
          <p class="summary-label">假資料範本</p>
          <h2 id="agreement-title">國立中央大學學務處場地借用切結與同意事項</h2>
        </div>
      </div>

      <div
        ref="agreementContentRef"
        class="agreement-content"
        tabindex="0"
        @scroll="handleAgreementScroll"
      >
        <section
          v-for="section in agreementSections"
          :key="section.title"
          class="agreement-section"
        >
          <div class="section-heading">
            <component :is="section.icon" :size="20" aria-hidden="true" />
            <h3>{{ section.title }}</h3>
          </div>
          <p>{{ section.description }}</p>
          <ul>
            <li v-for="item in section.items" :key="item">{{ item }}</li>
          </ul>
        </section>

        <section class="agreement-section signature-block">
          <div class="section-heading">
            <FilePenLine :size="20" aria-hidden="true" />
            <h3>聲明確認</h3>
          </div>
          <p>
            申請人確認本同意書為展示用假資料，實際借用規定仍以學務處公告、審核通知與現場管理人員說明為準。
          </p>
          <dl>
            <div>
              <dt>範例申請人</dt>
              <dd>測試生</dd>
            </div>
            <div>
              <dt>範例單位</dt>
              <dd>測試系學會</dd>
            </div>
            <div>
              <dt>範例日期</dt>
              <dd>2026 年 5 月 13 日</dd>
            </div>
          </dl>
        </section>
      </div>

      <div class="agreement-confirm">
        <label class="consent-checkbox" :class="{ 'is-disabled': !hasReadAgreement }">
          <input
            v-model="isConsentChecked"
            type="checkbox"
            :disabled="!hasReadAgreement"
          />
          <span>我已閱讀並同意遵守以上場地借用規範</span>
        </label>
        <p class="confirm-note">
          {{ hasReadAgreement ? "已完成條款閱讀，可勾選同意。" : "閱讀完同意書後即可勾選確認。" }}
        </p>
      </div>

      <div class="agreement-actions">
        <button
          type="button"
          class="btn btn-primary"
          :disabled="!canEnterSystem"
          @click="enterSystem"
        >
          <span class="btn-icon">
            <Check :size="16" aria-hidden="true" />
          </span>
          進入系統
        </button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import {
  Check,
  ClipboardCheck,
  FilePenLine,
  FileText,
  ShieldCheck,
  Undo2,
  Users,
  Wrench,
} from "lucide-vue-next";
import { useAuthSessionStore } from "@/stores/authSession";

const route = useRoute();
const router = useRouter();
const authSession = useAuthSessionStore();

const agreementContentRef = ref(null);
const hasReadAgreement = ref(false);
const isConsentChecked = ref(false);

const agreementSections = [
  {
    title: "場地借用目的與責任",
    icon: FileText,
    description: "申請人應依核准用途使用場地，並維持活動內容與申請資料一致。",
    items: [
      "借用場地限於核准日期、時段與範圍內使用，不得擅自轉借或變更用途。",
      "活動期間應維護場地整潔，結束後完成復原、垃圾清運與門窗電源檢查。",
      "若因申請資料不實或使用方式不當造成損害，申請人需配合後續處理。",
    ],
  },
  {
    title: "設備使用與損壞責任",
    icon: Wrench,
    description: "借用設備應依現場規範操作，並於活動結束後歸還至指定位置。",
    items: [
      "麥克風、投影設備、桌椅與延長線等物品須於借用前後清點數量。",
      "設備如有故障、短少或毀損，應立即通知管理單位，不得自行拆修。",
      "因不當操作造成設備損壞時，申請人須依管理單位認定負擔修復責任。",
    ],
  },
  {
    title: "活動安全與秩序",
    icon: ShieldCheck,
    description: "活動辦理期間應顧及參與者安全、校園秩序與周邊使用者權益。",
    items: [
      "不得阻塞逃生動線、消防設備、出入口或公共走道。",
      "使用音響、擴音或大型設備時，應控制音量並避免影響鄰近教學與辦公。",
      "若遇天災、停電或其他不可抗力情形，管理單位得調整或取消借用。",
    ],
  },
  {
    title: "取消、撤回與審核說明",
    icon: Undo2,
    description: "所有預約申請仍須經管理單位審核，通過後才視為完成借用程序。",
    items: [
      "申請送出後若需調整日期、時段、用途或設備，應於系統內修改或重新申請。",
      "審核中申請可依系統提供功能撤回，已通過申請如需取消應通知管理單位。",
      "管理單位得依場地維護、活動性質或校內需求保留核准與調整權利。",
    ],
  },
  {
    title: "個人資料使用聲明",
    icon: Users,
    description: "系統將使用申請資料進行場地審核、通知聯繫與借用紀錄管理。",
    items: [
      "姓名、電話、電子郵件與活動資訊僅供本系統展示、審核與管理流程使用。",
      "假資料範本不代表正式法務文字，正式版本應由業務單位確認後公告。",
      "申請人可於系統中查看自己的預約紀錄與審核狀態。",
    ],
  },
];

const canEnterSystem = computed(() => hasReadAgreement.value && isConsentChecked.value);

const getRedirectTarget = () => {
  const redirect = route.query.redirect;

  if (typeof redirect !== "string" || !redirect.startsWith("/")) {
    return authSession.getPostConsentRoute();
  }

  if (redirect.startsWith("/consent-agreement")) {
    return authSession.getPostConsentRoute();
  }

  if (redirect === "/") {
    return redirect;
  }

  return redirect;
};

const handleAgreementScroll = () => {
  const element = agreementContentRef.value;
  if (!element) return;

  const bottomOffset = element.scrollHeight - element.scrollTop - element.clientHeight;
  if (bottomOffset <= 2) {
    hasReadAgreement.value = true;
  }
};

const enterSystem = () => {
  if (!canEnterSystem.value) return;

  authSession.acceptConsentForCurrentUser();
  router.replace(getRedirectTarget());
};

onMounted(async () => {
  await nextTick();
  handleAgreementScroll();
});
</script>

<style lang="scss" scoped>
.consent-page {
  max-width: 960px;
  margin: 0 auto;
}

.consent-header {
  margin-bottom: 1.25rem;
  align-items: center;
  text-align: center;
}

.consent-header h1 {
  font-size: 3rem;
}

.hero-eyebrow,
.summary-label {
  margin: 0;
  color: var(--accent);
  font-size: var(--text-xs);
  font-weight: 800;
  text-transform: uppercase;
}

.agreement-panel {
  overflow: hidden;
}

.agreement-summary {
  display: flex;
  gap: 1rem;
  align-items: center;
  padding: 1.5rem;
  border-bottom: 1px solid var(--line);
  background: #fbfcfe;
}

.summary-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 3.25rem;
  height: 3.25rem;
  flex: 0 0 auto;
  border-radius: 50%;
  background: var(--accent-soft);
  color: var(--accent);
}

.agreement-summary h2 {
  margin-top: 0.25rem;
  color: var(--ink);
  font-size: var(--text-xl);
}

.agreement-content {
  max-height: min(56vh, 520px);
  overflow-y: auto;
  padding: 1.5rem;
  background: #ffffff;
  border-bottom: 1px solid var(--line);
}

.agreement-section {
  padding-bottom: 1.5rem;
  margin-bottom: 1.5rem;
  border-bottom: 1px solid var(--line);
}

.agreement-section:last-child {
  padding-bottom: 0;
  margin-bottom: 0;
  border-bottom: 0;
}

.section-heading {
  display: flex;
  align-items: center;
  gap: 0.55rem;
  color: var(--accent);
}

.section-heading h3 {
  color: var(--ink);
  font-size: var(--text-lg);
}

.agreement-section p {
  margin: 0.75rem 0 0;
  color: var(--muted-strong);
}

.agreement-section ul {
  display: grid;
  gap: 0.65rem;
  margin: 1rem 0 0;
  padding-left: 1.25rem;
  color: var(--text);
}

.signature-block dl {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.75rem;
  margin: 1rem 0 0;
}

.signature-block dl > div {
  padding: 0.9rem 1rem;
  border: 1px solid var(--line);
  border-radius: var(--radius-sm);
  background: #fbfcfe;
}

.signature-block dt {
  color: var(--muted);
  font-size: var(--text-xs);
  font-weight: 700;
}

.signature-block dd {
  margin: 0.25rem 0 0;
  color: var(--ink);
  font-weight: 800;
}

.agreement-confirm {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
  padding: 1.25rem 1.5rem;
  background: #fbfcfe;
  border-bottom: 1px solid var(--line);
}

.consent-checkbox {
  display: inline-flex;
  align-items: center;
  gap: 0.75rem;
  color: var(--ink);
  font-weight: 800;
  cursor: pointer;
}

.consent-checkbox.is-disabled {
  color: var(--muted);
  cursor: not-allowed;
}

.consent-checkbox input {
  width: 1.15rem;
  height: 1.15rem;
  accent-color: var(--accent);
}

.confirm-note {
  margin: 0;
  color: var(--muted);
  font-size: var(--text-sm);
  text-align: right;
}

.agreement-actions {
  display: flex;
  justify-content: flex-end;
  padding: 1.25rem 1.5rem;
  background: #ffffff;
}

.btn-icon {
  display: inline-flex;
  align-items: center;
}

@media (max-width: 720px) {
  .consent-header h1 {
    font-size: var(--text-4xl);
  }

  .agreement-summary,
  .agreement-confirm {
    align-items: flex-start;
    flex-direction: column;
  }

  .agreement-content {
    max-height: 54vh;
    padding: 1.25rem;
  }

  .signature-block dl {
    grid-template-columns: 1fr;
  }

  .confirm-note {
    text-align: left;
  }

  .agreement-actions {
    justify-content: stretch;
  }
}
</style>
