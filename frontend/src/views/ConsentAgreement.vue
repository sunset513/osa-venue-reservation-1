<template>
  <div class="consent-page page-enter">
    <header class="page-header consent-header">
      <p class="hero-eyebrow">Venue Use Consent</p>
      <h1>{{ t("pages.consent.heading") }}</h1>
      <p>{{ t("pages.consent.description") }}</p>
    </header>

    <section class="agreement-panel card" aria-labelledby="agreement-title">
      <div class="agreement-summary">
        <div class="summary-icon" aria-hidden="true">
          <ClipboardCheck :size="28" />
        </div>
        <div>
          <p class="summary-label">{{ t("pages.consent.sampleLabel") }}</p>
          <h2 id="agreement-title">{{ t("pages.consent.agreementTitle") }}</h2>
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
          :key="section.key"
          class="agreement-section"
        >
          <div class="section-heading">
            <component :is="section.icon" :size="20" aria-hidden="true" />
            <h3>{{ t(`${section.key}.title`) }}</h3>
          </div>
          <p>{{ t(`${section.key}.description`) }}</p>
          <ul>
            <li v-for="itemKey in section.itemKeys" :key="itemKey">
              {{ t(`${section.key}.${itemKey}`) }}
            </li>
          </ul>
        </section>

        <section class="agreement-section signature-block">
          <div class="section-heading">
            <FilePenLine :size="20" aria-hidden="true" />
            <h3>{{ t("pages.consent.confirmation.title") }}</h3>
          </div>
          <p>
            {{ t("pages.consent.confirmation.description") }}
          </p>
          <dl>
            <div>
              <dt>{{ t("pages.consent.confirmation.sampleApplicant") }}</dt>
              <dd>{{ t("pages.consent.confirmation.sampleApplicantValue") }}</dd>
            </div>
            <div>
              <dt>{{ t("pages.consent.confirmation.sampleUnit") }}</dt>
              <dd>{{ t("pages.consent.confirmation.sampleUnitValue") }}</dd>
            </div>
            <div>
              <dt>{{ t("pages.consent.confirmation.sampleDate") }}</dt>
              <dd>{{ t("pages.consent.confirmation.sampleDateValue") }}</dd>
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
          <span>{{ t("pages.consent.agreeLabel") }}</span>
        </label>
        <p class="confirm-note">
          {{ hasReadAgreement ? t("pages.consent.readComplete") : t("pages.consent.readRequired") }}
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
          {{ t("pages.consent.enterSystem") }}
        </button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useI18n } from "vue-i18n";
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
const { t } = useI18n();

const agreementContentRef = ref(null);
const hasReadAgreement = ref(false);
const isConsentChecked = ref(false);

const agreementSections = [
  {
    key: "pages.consent.sections.responsibility",
    icon: FileText,
    itemKeys: ["item1", "item2", "item3"],
  },
  {
    key: "pages.consent.sections.equipment",
    icon: Wrench,
    itemKeys: ["item1", "item2", "item3"],
  },
  {
    key: "pages.consent.sections.safety",
    icon: ShieldCheck,
    itemKeys: ["item1", "item2", "item3"],
  },
  {
    key: "pages.consent.sections.review",
    icon: Undo2,
    itemKeys: ["item1", "item2", "item3"],
  },
  {
    key: "pages.consent.sections.privacy",
    icon: Users,
    itemKeys: ["item1", "item2", "item3"],
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
