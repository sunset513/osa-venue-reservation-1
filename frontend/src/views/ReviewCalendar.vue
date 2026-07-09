<template>
  <div ref="reviewPageRef" class="review-page history-page page-enter">
    <header class="workbench-header">
      <div class="header-copy">
        <span class="mode-pill">
          <ShieldCheck :size="16" aria-hidden="true" />
          {{ reviewCopy.mode }}
        </span>
        <p class="header-eyebrow">Review Workbench</p>
        <h1 class="page-title">
          <ClipboardCheck :size="28" aria-hidden="true" class="page-title-icon" />
          <span>{{ reviewCopy.title }}</span>
        </h1>
        <p>{{ reviewCopy.description }}</p>
      </div>

      <div class="header-actions">
        <div class="review-mode-toggle" role="group" :aria-label="reviewCopy.switchMode">
          <button
            class="view-toggle-btn badge-toggle-btn"
            :class="{ 'is-active': activeReviewMode === 'venue' }"
            type="button"
            @click="activeReviewMode = 'venue'"
          >
            <Building2 :size="16" aria-hidden="true" />
            <span>{{ reviewCopy.venueMode }}</span>
            <span v-if="venuePendingCount > 0" class="pending-badge">
              {{ venuePendingCount }}
            </span>
          </button>
          <button
            class="view-toggle-btn badge-toggle-btn"
            :class="{ 'is-active': activeReviewMode === 'equipment' }"
            type="button"
            @click="activeReviewMode = 'equipment'"
          >
            <Wrench :size="16" aria-hidden="true" />
            <span>{{ reviewCopy.equipmentMode }}</span>
            <span
              v-if="equipmentPendingCount > 0"
              class="pending-badge pending-badge--dot"
              aria-hidden="true"
            ></span>
          </button>
        </div>

        <button
          v-if="isReviewer"
          class="btn admin-equipment-btn"
          type="button"
          @click="navigateToEquipmentStatus"
        >
          <Wrench :size="17" aria-hidden="true" />
          <span>{{ reviewCopy.equipmentAdmin }}</span>
        </button>
      </div>
    </header>

    <div v-if="pageLoading" class="loading-state">{{ reviewCopy.loading }}</div>

    <div v-if="!pageLoading && activeReviewMode === 'venue'">
      <div ref="reviewStickyStackRef" class="review-sticky-stack" :class="{ 'is-stuck': isReviewStickyPinned }">
        <div class="review-mode-toggle-row">
          <div class="review-mode-toggle" role="group" :aria-label="reviewCopy.switchMode">
            <button
              class="view-toggle-btn badge-toggle-btn"
              :class="{ 'is-active': activeReviewMode === 'venue' }"
              type="button"
              @click="activeReviewMode = 'venue'"
            >
              <Building2 :size="16" aria-hidden="true" />
              <span>{{ reviewCopy.venueMode }}</span>
              <span v-if="venuePendingCount > 0" class="pending-badge">
                {{ venuePendingCount }}
              </span>
            </button>
            <button
              class="view-toggle-btn badge-toggle-btn"
              :class="{ 'is-active': activeReviewMode === 'equipment' }"
              type="button"
              @click="activeReviewMode = 'equipment'"
            >
              <Wrench :size="16" aria-hidden="true" />
              <span>{{ reviewCopy.equipmentMode }}</span>
              <span
                v-if="equipmentPendingCount > 0"
                class="pending-badge pending-badge--dot"
                aria-hidden="true"
              ></span>
            </button>
          </div>
        </div>

        <div class="panel-heading">
          <div>
            <p class="panel-kicker">{{ reviewCopy.currentVenue }}</p>
            <h2>{{ selectedVenueName }}</h2>
            <p class="panel-note">{{ reviewCopy.venueNote }}</p>
            <div style="margin-top: 12px;">
              <button
                class="btn btn-secondary route-booking-btn"
                type="button"
                :disabled="!canNavigateToVenueBooking"
                @click="navigateToVenueBooking()"
              >
                <ArrowRight :size="17" aria-hidden="true" />
                <span>{{ bookingRouteLabel }}</span>
              </button>
            </div>
          </div>
          <div class="panel-heading-actions">
            <div class="view-toggle" role="group" :aria-label="reviewCopy.switchView">
              <button
                class="view-toggle-btn"
                :class="{ 'is-active': activeViewMode === 'calendar' }"
                type="button"
                :aria-pressed="activeViewMode === 'calendar'"
                @click="activeViewMode = 'calendar'"
              >
                <CalendarDays :size="16" aria-hidden="true" />
                <span>{{ reviewCopy.calendar }}</span>
              </button>
              <button
                class="view-toggle-btn"
                :class="{ 'is-active': activeViewMode === 'list' }"
                type="button"
                :aria-pressed="activeViewMode === 'list'"
                @click="activeViewMode = 'list'"
              >
                <List :size="16" aria-hidden="true" />
                <span>{{ reviewCopy.list }}</span>
              </button>
            </div>
            <label class="quick-status-filter" for="review-sort-quick">
              <span>{{ reviewCopy.sort }}</span>
              <select id="review-sort-quick" v-model="selectedSort" :disabled="isFetchingEvents" @change="handleSortChange">
                <option v-for="option in reviewSortOptions" :key="option.value" :value="option.value">
                  {{ getReviewSortLabel(option) }}
                </option>
              </select>
            </label>
          </div>
        </div>
      </div>

      <div class="workbench-layout">
        <aside class="control-panel card">
          <section class="panel-section">
            <label for="review-venue">{{ reviewCopy.venueSelectorLabel }}</label>
            <select id="review-venue" v-model="selectedVenueId" @change="handleFilterChange">
              <option :value="ALL_VENUES_VALUE">{{ reviewCopy.allVenues }}</option>
              <option v-for="venue in venues" :key="venue.id" :value="venue.id">
                {{ venue.name }}
              </option>
            </select>
          </section>

          <section v-if="activeViewMode === 'list'" class="panel-section review-filter-panel">
            <div class="review-filter-toolbar">
              <div class="filter-field">
                <label for="review-list-keyword">{{ reviewCopy.keywordLabel }}</label>
                <input
                  id="review-list-keyword"
                  v-model.trim="venueListFilters.keyword"
                  type="text"
                  :placeholder="reviewCopy.venueKeywordPlaceholder"
                />
              </div>

              <div ref="venueDateRangePickerRef" class="date-range-picker review-date-range-picker">
                <label for="review-list-date-range-trigger">{{ reviewCopy.dateRangeLabel }}</label>
                <button
                  id="review-list-date-range-trigger"
                  type="button"
                  class="date-range-trigger"
                  :class="{ 'is-open': venueListFilters.datePickerOpen }"
                  :aria-expanded="venueListFilters.datePickerOpen"
                  aria-controls="review-list-date-range-popover"
                  @click="toggleReviewDatePicker(venueListFilters)"
                >
                  <span class="date-range-segment" :class="{ 'has-value': venueListFilters.startDate }">
                    <span class="date-range-label">{{ reviewCopy.dateStart }}</span>
                    <strong>{{ formatDatePickerLabel(venueListFilters.startDate) }}</strong>
                  </span>
                  <span class="date-range-segment" :class="{ 'has-value': venueListFilters.endDate }">
                    <span class="date-range-label">{{ reviewCopy.dateEnd }}</span>
                    <strong>{{ formatDatePickerLabel(venueListFilters.endDate) }}</strong>
                  </span>
                  <ChevronDown :size="18" class="date-range-chevron" aria-hidden="true" />
                </button>

                <button
                  v-if="venueListFilters.startDate || venueListFilters.endDate"
                  type="button"
                  class="date-range-clear"
                  @click.stop="clearReviewDateRange(venueListFilters)"
                >
                  {{ reviewCopy.clearDate }}
                </button>

                <Teleport to="body">
                  <div
                    v-if="venueListFilters.datePickerOpen"
                    id="review-list-date-range-popover"
                    ref="venueDateRangePopoverRef"
                    class="date-range-popover"
                  >
                    <div class="calendar-selection-footer">
                      <div class="calendar-selection-summary" aria-live="polite">
                        <span class="calendar-selection-label">{{ reviewCopy.currentRange }}</span>
                        <strong>{{ getReviewFilterRangeLabel(venueListFilters) }}</strong>
                        <span class="calendar-selection-hint">{{ getReviewFilterRangeHint(venueListFilters) }}</span>
                      </div>
                      <div class="calendar-manual-inputs">
                        <label class="calendar-manual-field">
                          <span>{{ reviewCopy.startDateLabel }}</span>
                          <input
                            type="date"
                            :value="venueListFilters.startDate"
                            @change="updateReviewFilterStartDate(venueListFilters, $event.target.value)"
                          />
                        </label>
                        <label class="calendar-manual-field">
                          <span>{{ reviewCopy.endDateLabel }}</span>
                          <input
                            type="date"
                            :value="venueListFilters.endDate"
                            @change="updateReviewFilterEndDate(venueListFilters, $event.target.value)"
                          />
                        </label>
                      </div>
                    </div>
                  </div>
                </Teleport>
              </div>

              <div class="filter-summary">
                <span class="summary-label">{{ reviewCopy.currentResultsLabel }}</span>
                <strong>{{ reviewListBookings.length }} {{ reviewCopy.itemsUnit }}</strong>
                <button
                  v-if="venueListHasActiveFilters"
                  type="button"
                  class="clear-filter-btn"
                  @click="clearVenueListFilters"
                >
                  {{ reviewCopy.clearFilters }}
                </button>
              </div>
            </div>
          </section>

        </aside>

        <section class="calendar-panel card">
          <div class="filter-tabs record-tabs" role="tablist" :aria-label="reviewCopy.venueStatusAria">
            <button
              v-for="option in venueReviewStatusTabs"
              :key="option.key"
              type="button"
              class="filter-tab"
              :class="{ 'is-active': selectedStatus === option.statusValue }"
              role="tab"
              :aria-selected="selectedStatus === option.statusValue"
              @click="selectStatusFilter(option.statusValue)"
            >
              <component
                :is="option.icon"
                v-if="option.icon"
                :size="18"
                class="filter-tab-icon"
                aria-hidden="true"
              />
              {{ option.label }}
              <span class="tab-count" style="font-size: 0.85em; opacity: 0.7;">({{ option.value }})</span>
            </button>
          </div>

          <div
            v-show="activeViewMode === 'calendar'"
            class="calendar-shell"
            :class="{ 'is-loading': isFetchingEvents }"
            @keydown.esc="closeMonthPicker"
          >
            <div v-if="isMonthPickerOpen" class="month-picker-popover" role="dialog" aria-label="選擇月份">
              <label for="review-month-picker">選擇月份</label>
              <input
                id="review-month-picker"
                ref="monthPickerRef"
                v-model="monthPickerValue"
                type="month"
                @keyup.enter="goToSelectedMonth"
              />
              <button class="month-picker-action is-primary" type="button" @click="goToSelectedMonth">
                套用
              </button>
              <button class="month-picker-action" type="button" @click="closeMonthPicker">
                取消
              </button>
            </div>
            <FullCalendar ref="calendarRef" :options="calendarOptions" />
          </div>

          <div v-show="activeViewMode === 'list'" class="list-shell" :class="{ 'is-loading': isFetchingEvents }">
            <div v-if="reviewListBookings.length === 0" class="list-empty-state">
              {{ reviewCopy.venueEmpty }}
            </div>

            <div v-else class="case-list">
              <button
                v-for="booking in paginatedReviewListBookings"
                :key="booking.id"
                class="case-row"
                type="button"
                @click="openBookingDetail(booking.id)"
              >
                <div class="case-main">
                  <div class="case-title-line">
                    <span class="status-pill" :class="booking.statusClass">{{ booking.statusText }}</span>
                    <strong>{{ booking.purpose || reviewCopy.noPurpose }}</strong>
                  </div>
                  <div class="case-meta">
                    <span class="case-id-pill">{{ reviewCopy.venueBookingIdPrefix }} #{{ booking.id }}</span>
                    <span>{{ booking.venueName }}</span>
                    <span>{{ booking.contactName }}</span>
                    <span>{{ booking.participantCount }} {{ reviewCopy.peopleUnit }}</span>
                  </div>
                </div>
                <div class="case-schedule-right">
                  <div class="schedule-date">{{ booking.bookingDate }}</div>
                  <div class="schedule-time">{{ booking.timeRange || reviewCopy.noTimeRange }}</div>
                </div>
              </button>
            </div>

            <nav
              v-if="venueReviewTotalPages > 1"
              class="pagination-bar"
              :aria-label="reviewCopy.venuePaginationAria"
            >
              <p class="pagination-summary">
                {{ reviewCopy.pageSummaryPrefix }} {{ venueReviewPaginationStartIndex }} - {{ venueReviewPaginationEndIndex }} {{ reviewCopy.itemsUnit }}
                / {{ reviewCopy.pageSummaryMiddle }} {{ reviewListBookings.length }} {{ reviewCopy.itemsUnit }}
              </p>
              <div class="pagination-controls">
                <button
                  type="button"
                  class="pagination-btn"
                  :disabled="!canGoPreviousVenueReviewPage"
                  :aria-label="reviewCopy.previousPage"
                  :title="reviewCopy.previousPage"
                  @click="goToPreviousVenueReviewPage"
                >
                  <ChevronLeft :size="17" aria-hidden="true" />
                </button>
                <button
                  v-for="pageNo in visibleVenueReviewPageNumbers"
                  :key="pageNo"
                  type="button"
                  class="pagination-page"
                  :class="{ 'is-active': venueReviewCurrentPage === pageNo }"
                  :aria-current="venueReviewCurrentPage === pageNo ? 'page' : undefined"
                  @click="setVenueReviewPage(pageNo)"
                >
                  {{ pageNo }}
                </button>
                <button
                  type="button"
                  class="pagination-btn"
                  :disabled="!canGoNextVenueReviewPage"
                  :aria-label="reviewCopy.nextPage"
                  :title="reviewCopy.nextPage"
                  @click="goToNextVenueReviewPage"
                >
                  <ChevronRight :size="17" aria-hidden="true" />
                </button>
              </div>
            </nav>
          </div>
        </section>
      </div>
    </div>
    <div v-else-if="!pageLoading && activeReviewMode === 'equipment'">
      <div ref="reviewStickyStackRef" class="review-sticky-stack" :class="{ 'is-stuck': isReviewStickyPinned }">
        <div class="review-mode-toggle-row">
          <div class="review-mode-toggle" role="group" :aria-label="reviewCopy.switchMode">
            <button
              class="view-toggle-btn badge-toggle-btn"
              :class="{ 'is-active': activeReviewMode === 'venue' }"
              type="button"
              @click="activeReviewMode = 'venue'"
            >
              <Building2 :size="16" aria-hidden="true" />
              <span>{{ reviewCopy.venueMode }}</span>
              <span v-if="venuePendingCount > 0" class="pending-badge">
                {{ venuePendingCount }}
              </span>
            </button>
            <button
              class="view-toggle-btn badge-toggle-btn"
              :class="{ 'is-active': activeReviewMode === 'equipment' }"
              type="button"
              @click="activeReviewMode = 'equipment'"
            >
              <Wrench :size="16" aria-hidden="true" />
              <span>{{ reviewCopy.equipmentMode }}</span>
              <span
                v-if="equipmentPendingCount > 0"
                class="pending-badge pending-badge--dot"
                aria-hidden="true"
              ></span>
            </button>
          </div>
        </div>

        <div class="panel-heading">
          <div>
            <p class="panel-kicker">{{ reviewCopy.equipmentMode }}</p>
            <h2>{{ reviewCopy.equipmentTitle }}</h2>
            <p class="panel-note">{{ reviewCopy.equipmentNote }}</p>
            <div style="margin-top: 12px;">
              <button
                class="btn btn-secondary route-booking-btn"
                type="button"
                :disabled="!canNavigateToVenueBooking"
                @click="navigateToVenueBooking()"
              >
                <ArrowRight :size="17" aria-hidden="true" />
                <span>{{ bookingRouteLabel }}</span>
              </button>
            </div>
          </div>
          <div class="panel-heading-actions">
            <label class="quick-status-filter" for="equipment-sort-quick">
              <span>{{ reviewCopy.sort }}</span>
              <select id="equipment-sort-quick" v-model="equipmentSelectedSort" :disabled="equipmentReviewLoading">
                <option v-for="option in reviewSortOptions" :key="option.value" :value="option.value">
                  {{ getReviewSortLabel(option) }}
                </option>
              </select>
            </label>
          </div>
        </div>
      </div>
      <div class="workbench-layout">
        <aside class="control-panel card">
          <section class="panel-section review-filter-panel">
            <div class="review-filter-toolbar">
              <div class="filter-field">
                <label for="equipment-review-keyword">{{ reviewCopy.keywordLabel }}</label>
                <input
                  id="equipment-review-keyword"
                  v-model.trim="equipmentFilters.keyword"
                  type="text"
                  :placeholder="reviewCopy.equipmentKeywordPlaceholder"
                />
              </div>

              <div ref="equipmentDateRangePickerRef" class="date-range-picker review-date-range-picker">
                <label for="equipment-review-date-range-trigger">{{ reviewCopy.dateRangeLabel }}</label>
                <button
                  id="equipment-review-date-range-trigger"
                  type="button"
                  class="date-range-trigger"
                  :class="{ 'is-open': equipmentFilters.datePickerOpen }"
                  :aria-expanded="equipmentFilters.datePickerOpen"
                  aria-controls="equipment-review-date-range-popover"
                  @click="toggleReviewDatePicker(equipmentFilters)"
                >
                  <span class="date-range-segment" :class="{ 'has-value': equipmentFilters.startDate }">
                    <span class="date-range-label">{{ reviewCopy.dateStart }}</span>
                    <strong>{{ formatDatePickerLabel(equipmentFilters.startDate) }}</strong>
                  </span>
                  <span class="date-range-segment" :class="{ 'has-value': equipmentFilters.endDate }">
                    <span class="date-range-label">{{ reviewCopy.dateEnd }}</span>
                    <strong>{{ formatDatePickerLabel(equipmentFilters.endDate) }}</strong>
                  </span>
                  <ChevronDown :size="18" class="date-range-chevron" aria-hidden="true" />
                </button>

                <button
                  v-if="equipmentFilters.startDate || equipmentFilters.endDate"
                  type="button"
                  class="date-range-clear"
                  @click.stop="clearReviewDateRange(equipmentFilters)"
                >
                  {{ reviewCopy.clearDate }}
                </button>

                <Teleport to="body">
                  <div
                    v-if="equipmentFilters.datePickerOpen"
                    id="equipment-review-date-range-popover"
                    ref="equipmentDateRangePopoverRef"
                    class="date-range-popover"
                  >
                    <div class="calendar-selection-footer">
                      <div class="calendar-selection-summary" aria-live="polite">
                        <span class="calendar-selection-label">{{ reviewCopy.currentRange }}</span>
                        <strong>{{ getReviewFilterRangeLabel(equipmentFilters) }}</strong>
                        <span class="calendar-selection-hint">{{ getReviewFilterRangeHint(equipmentFilters) }}</span>
                      </div>
                      <div class="calendar-manual-inputs">
                        <label class="calendar-manual-field">
                          <span>{{ reviewCopy.startDateLabel }}</span>
                          <input
                            type="date"
                            :value="equipmentFilters.startDate"
                            @change="updateReviewFilterStartDate(equipmentFilters, $event.target.value)"
                          />
                        </label>
                        <label class="calendar-manual-field">
                          <span>{{ reviewCopy.endDateLabel }}</span>
                          <input
                            type="date"
                            :value="equipmentFilters.endDate"
                            @change="updateReviewFilterEndDate(equipmentFilters, $event.target.value)"
                          />
                        </label>
                      </div>
                    </div>
                  </div>
                </Teleport>
              </div>

              <div class="filter-summary">
                <span class="summary-label">{{ reviewCopy.currentResultsLabel }}</span>
                <strong>{{ filteredEquipmentReviewItems.length }} {{ reviewCopy.itemsUnit }}</strong>
                <button
                  v-if="equipmentHasActiveFilters"
                  type="button"
                  class="clear-filter-btn"
                  @click="clearEquipmentFilters"
                >
                  {{ reviewCopy.clearFilters }}
                </button>
              </div>
            </div>
          </section>

        </aside>

        <section class="calendar-panel card">
          <div class="filter-tabs record-tabs" role="tablist" :aria-label="reviewCopy.statusLabel">
            <button
              v-for="option in equipmentReviewStatusTabs"
              :key="option.key"
              type="button"
              class="filter-tab"
              :class="{ 'is-active': equipmentSelectedStatus === option.statusValue }"
              role="tab"
              :aria-selected="equipmentSelectedStatus === option.statusValue"
              @click="selectEquipmentStatusFilter(option.statusValue)"
            >
              <component
                :is="option.icon"
                v-if="option.icon"
                :size="18"
                class="filter-tab-icon"
                aria-hidden="true"
              />
              {{ option.label }}
              <span class="tab-count" style="font-size: 0.85em; opacity: 0.7;">({{ option.value }})</span>
            </button>
          </div>

          <div class="list-shell" :class="{ 'is-loading': equipmentReviewLoading }">
            <div v-if="filteredEquipmentReviewItems.length === 0" class="list-empty-state">
              {{ reviewCopy.equipmentEmpty }}
            </div>
            <div v-else class="case-list">
            <button
              v-for="equipmentBooking in paginatedEquipmentReviewItems"
              :key="equipmentBooking.id"
              class="case-row"
              type="button"
              @click="openEquipmentReviewTarget(equipmentBooking)"
            >
              <div class="case-main">
                <div class="case-title-line">
                  <span class="status-pill" :class="getReviewEquipmentStatusDisplayMeta(equipmentBooking.status).className">
                    {{ getReviewEquipmentStatusDisplayMeta(equipmentBooking.status).text }}
                  </span>
                  <strong>{{ equipmentBooking.itemSummary }}</strong>
                </div>
                <div class="case-meta">
                  <span class="case-id-pill case-id-pill--equipment">{{ reviewCopy.equipmentBookingIdPrefix }} #{{ equipmentBooking.id }}</span>
                  <template v-if="equipmentBooking.relatedVenueBookingId">
                    <span>{{ equipmentBooking.relatedVenueBookingTitle || reviewCopy.noPurpose }}</span>
                  </template>
                  <span>{{ equipmentBooking.contact.name || equipmentBooking.userId || reviewCopy.noApplicant }}</span>
                </div>
              </div>
              <div class="case-schedule-right">
                <div class="schedule-date">{{ formatEquipmentBorrowDateMeta(equipmentBooking.borrowDate) }}</div>
                <div class="schedule-time">{{ equipmentBooking.timeRange || reviewCopy.noTimeRange }}</div>
              </div>
            </button>
          </div>

          <nav
            v-if="equipmentReviewTotalPages > 1"
            class="pagination-bar"
            :aria-label="reviewCopy.equipmentPaginationAria"
          >
            <p class="pagination-summary">
              {{ reviewCopy.pageSummaryPrefix }} {{ equipmentReviewPaginationStartIndex }} - {{ equipmentReviewPaginationEndIndex }} {{ reviewCopy.itemsUnit }}
              / {{ reviewCopy.pageSummaryMiddle }} {{ filteredEquipmentReviewItems.length }} {{ reviewCopy.itemsUnit }}
            </p>
            <div class="pagination-controls">
              <button
                type="button"
                class="pagination-btn"
                :disabled="!canGoPreviousEquipmentReviewPage"
                :aria-label="reviewCopy.previousPage"
                :title="reviewCopy.previousPage"
                @click="goToPreviousEquipmentReviewPage"
              >
                <ChevronLeft :size="17" aria-hidden="true" />
              </button>
              <button
                v-for="pageNo in visibleEquipmentReviewPageNumbers"
                :key="pageNo"
                type="button"
                class="pagination-page"
                :class="{ 'is-active': equipmentReviewCurrentPage === pageNo }"
                :aria-current="equipmentReviewCurrentPage === pageNo ? 'page' : undefined"
                @click="setEquipmentReviewPage(pageNo)"
              >
                {{ pageNo }}
              </button>
              <button
                type="button"
                class="pagination-btn"
                :disabled="!canGoNextEquipmentReviewPage"
                :aria-label="reviewCopy.nextPage"
                :title="reviewCopy.nextPage"
                @click="goToNextEquipmentReviewPage"
              >
                <ChevronRight :size="17" aria-hidden="true" />
              </button>
            </div>
          </nav>
          </div>
        </section>
      </div>
    </div>
  </div>

  <ReviewDayScheduleModal
    :visible="isDayModalVisible"
    :selectedDate="selectedDate"
    :dayOfWeek="selectedDayOfWeek"
    :bookings="selectedDayBookings"
    :can-create-booking="canNavigateToVenueBooking"
    @close="closeDayModal"
    @open-detail="openBookingDetail"
    @create-booking="navigateToVenueBooking(selectedDate)"
  />

  <ReviewBookingModal
    :visible="isDetailModalVisible"
    :booking="selectedBookingDetail"
    :loading="detailLoading"
    :processing="detailProcessing"
    :equipment-bookings="selectedEquipmentBookings"
    :equipment-loading="equipmentDetailLoading"
    @close="closeDetailModal"
    @approve="handleApprove"
    @update-status="handleStatusUpdate"
  />

  <ReviewEquipmentModal
    :visible="isEquipmentDetailModalVisible"
    :booking="selectedEquipmentBookingDetail"
    :processing="equipmentProcessingId !== null"
    @close="closeEquipmentDetailModal"
    @update-status="handleEquipmentDetailStatusUpdate"
  />
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import FullCalendar from "@fullcalendar/vue3";
import dayGridPlugin from "@fullcalendar/daygrid";
import interactionPlugin from "@fullcalendar/interaction";
import {
  ArrowRight,
  BadgeCheck,
  Building2,
  CalendarDays,
  Check,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  ClipboardCheck,
  ClipboardList,
  Clock3,
  List,
  RotateCcw,
  ShieldCheck,
  Wrench,
  XCircle,
} from "lucide-vue-next";

import ReviewBookingModal from "@/components/review/ReviewBookingModal.vue";
import ReviewDayScheduleModal from "@/components/review/ReviewDayScheduleModal.vue";
import ReviewEquipmentModal from "@/components/review/ReviewEquipmentModal.vue";
import { fetchVenuesByUnit } from "@/api/venue";
import {
  approveReviewBooking,
  fetchPendingReviews,
  fetchReviewBookingDetail,
  updateReviewBookingStatus,
} from "@/api/review";
import {
  getEquipmentReviewsByVenueBooking,
  queryEquipmentReviews,
  updateEquipmentReviewStatus,
} from "@/api/equipment";
import {
  convertSlotsToTimeRange,
  formatSlotsAsTimeRange,
  formatSlotGroupsAsTimeRange,
  getReviewEventColorConfig,
  groupContiguousSlots,
} from "@/utils/dateHelper";
import { formatDateKey, getDailyEventCount, renderMoreLinkContent } from "@/utils/calendarDisplay";
import { getBookingStatusMeta, parseContactInfo } from "@/utils/bookingMeta";
import {
  getEquipmentBookingStatusMeta,
  getEquipmentReviewOpenTarget,
  normalizeEquipmentBooking,
  normalizeEquipmentBookingPage,
} from "@/utils/equipment";
import {
  countReviewStatuses,
  filterEquipmentReviewList,
  filterVenueReviewList,
  hasActiveReviewFilters,
} from "@/utils/reviewFilters";
import { parseReviewRouteQuery } from "@/utils/reviewRouteQuery";
import { useAuthSessionStore } from "@/stores/authSession";
import { useToast } from "@/utils/useToast";

const { success, error } = useToast();
const route = useRoute();
const router = useRouter();
const authSession = useAuthSessionStore();
const reviewCopy = {
  mode: "\u5be9\u6838\u8005\u6a21\u5f0f",
  title: "\u5be9\u6838\u5de5\u4f5c\u53f0",
  description: "\u4ee5\u9810\u7d04\u7533\u8acb\u70ba\u4e2d\u5fc3\u8655\u7406\u5834\u5730\u501f\u7528\uff0c\u6aa2\u8996\u72c0\u614b\u3001\u6bd4\u5c0d\u6642\u6bb5\uff0c\u4e26\u5f9e\u540c\u4e00\u8655\u5b8c\u6210\u901a\u904e\u6216\u9000\u56de\u3002",
  switchMode: "\u5207\u63db\u5be9\u6838\u985e\u578b",
  switchView: "\u5207\u63db\u9810\u7d04\u7533\u8acb\u6aa2\u8996",
  venueMode: "\u5834\u5730\u9810\u7d04",
  equipmentMode: "\u8a2d\u5099\u501f\u7528",
  equipmentAdmin: "\u8a2d\u5099\u72c0\u614b\u7ba1\u7406",
  currentVenue: "\u76ee\u524d\u5834\u5730",
  venueNote: "\u901a\u904e\u95dc\u806f\u5834\u5730\u9810\u7d04\u7533\u8acb\u6642\uff0c\u7cfb\u7d71\u4e5f\u6703\u4e00\u4f75\u901a\u904e\u8a72\u7b46\u8a2d\u5099\u501f\u7528\u7533\u8acb\u3002",
  equipmentTitle: "\u8a2d\u5099\u5be9\u6838\u6e05\u55ae",
  equipmentNote: "\u901a\u904e\u95dc\u806f\u5834\u5730\u9810\u7d04\u7533\u8acb\u6642\uff0c\u7cfb\u7d71\u4e5f\u6703\u4e00\u4f75\u901a\u904e\u8a72\u7b46\u8a2d\u5099\u501f\u7528\u7533\u8acb\u3002",
  venueSelectorLabel: "\u5be9\u6838\u5834\u5730",
  allVenues: "\u5168\u90e8\u5834\u5730",
  keywordLabel: "\u95dc\u9375\u5b57\u641c\u5c0b",
  venueKeywordPlaceholder: "\u641c\u5c0b\u7528\u9014\u3001\u5834\u5730\u3001\u7533\u8acb\u4eba\u6216\u7de8\u865f",
  equipmentKeywordPlaceholder: "\u641c\u5c0b\u8a2d\u5099\u3001\u7528\u9014\u3001\u7533\u8acb\u4eba\u6216\u7de8\u865f",
  dateRangeLabel: "\u65e5\u671f\u5340\u9593",
  equipmentDateRangeLabel: "\u501f\u7528\u65e5\u671f\u5340\u9593",
  dateStart: "\u958b\u59cb",
  dateEnd: "\u7d50\u675f",
  clearDate: "\u6e05\u9664\u65e5\u671f",
  currentRange: "\u76ee\u524d\u5340\u9593",
  startDateLabel: "\u958b\u59cb\u65e5\u671f",
  endDateLabel: "\u7d50\u675f\u65e5\u671f",
  currentResultsLabel: "\u76ee\u524d\u7d50\u679c",
  clearFilters: "\u6e05\u9664\u7be9\u9078",
  venueStatusAria: "\u5834\u5730\u7533\u8acb\u72c0\u614b\u7be9\u9078",
  venueTabsAria: "\u5834\u5730\u5be9\u6838\u72c0\u614b\u7be9\u9078",
  equipmentTabsAria: "\u8a2d\u5099\u5be9\u6838\u72c0\u614b\u7be9\u9078",
  calendar: "\u6708\u66c6",
  list: "\u5217\u8868",
  sort: "\u6392\u5e8f\u65b9\u5f0f",
  loading: "\u8f09\u5165\u5834\u5730\u8207\u5be9\u6838\u8cc7\u6599\u4e2d...",
  statusLabel: "\u7533\u8acb\u72c0\u614b",
  equipmentStatusLabel: "\u8a2d\u5099\u7533\u8acb\u72c0\u614b\u7be9\u9078",
  venueEmpty: "\u76ee\u524d\u7be9\u9078\u689d\u4ef6\u4e0b\u6c92\u6709\u9810\u7d04\u7533\u8acb\u3002",
  equipmentEmpty: "\u76ee\u524d\u6c92\u6709\u8a2d\u5099\u501f\u7528\u7533\u8acb\u3002",
  equipmentLoading: "\u8f09\u5165\u8a2d\u5099\u7533\u8acb\u4e2d...",
  noPurpose: "\u672a\u586b\u5beb\u7528\u9014",
  noTimeRange: "\u672a\u6307\u5b9a\u6642\u6bb5",
  noApplicant: "\u672a\u63d0\u4f9b\u7533\u8acb\u4eba",
  venueBookingIdPrefix: "\u5834\u5730\u9810\u7d04\u7de8\u865f",
  equipmentBookingIdPrefix: "\u8a2d\u5099\u501f\u7528\u7de8\u865f",
  peopleUnit: "\u4eba",
  itemsUnit: "\u7b46",
  previousPage: "\u4e0a\u4e00\u9801",
  nextPage: "\u4e0b\u4e00\u9801",
  venuePaginationAria: "\u5834\u5730\u5be9\u6838\u5217\u8868\u5206\u9801",
  equipmentPaginationAria: "\u8a2d\u5099\u5be9\u6838\u5217\u8868\u5206\u9801",
  pageSummaryPrefix: "\u7b2c",
  pageSummaryMiddle: "\u5171",
};
const ALL_VENUES_VALUE = "all";
const REVIEW_UNIT_ID = "1";
const REVIEW_PAGE_SIZE = 100;
const REVIEW_LIST_PAGE_SIZE = 10;
const reviewSortOptions = [
  { value: "date-desc", label: "\u65e5\u671f\u7531\u65b0\u5230\u820a" },
  { value: "date-asc", label: "\u65e5\u671f\u7531\u820a\u5230\u65b0" },
  { value: "id-desc", label: "\u7de8\u865f\u7531\u65b0\u5230\u820a" },
  { value: "id-asc", label: "\u7de8\u865f\u7531\u820a\u5230\u65b0" },
];

const reviewSortLabelMap = {
  "date-desc": "\u65e5\u671f\u7531\u65b0\u5230\u820a",
  "date-asc": "\u65e5\u671f\u7531\u820a\u5230\u65b0",
  "id-desc": "\u7de8\u865f\u7531\u65b0\u5230\u820a",
  "id-asc": "\u7de8\u865f\u7531\u820a\u5230\u65b0",
};

const getReviewSortLabel = (option) => reviewSortLabelMap[option.value] || option.label;


const createReviewFilterState = () => reactive({
  keyword: "",
  startDate: "",
  endDate: "",
  datePickerOpen: false,
});

const calendarRef = ref(null);
const reviewPageRef = ref(null);
const reviewStickyStackRef = ref(null);
const venues = ref([]);
const selectedVenueId = ref(ALL_VENUES_VALUE);
const selectedStatus = ref("1");
const selectedSort = ref("date-desc");
const pageLoading = ref(true);
const isFetchingEvents = ref(false);
const activeViewMode = ref("list");
const activeReviewMode = ref("venue");
const equipmentSelectedStatus = ref("1");
const equipmentSelectedSort = ref("date-desc");
const venueReviewCurrentPage = ref(1);
const equipmentReviewCurrentPage = ref(1);
const isMonthPickerOpen = ref(false);
const monthPickerValue = ref("");
const monthPickerRef = ref(null);
const currentCalendarVisibleRange = ref({
  startDate: "",
  endDate: "",
});
const events = ref([]);
const monthlyBookings = ref([]);
const allMonthlyBookings = ref([]);
const venueListBookingsSource = ref([]);
const isDayModalVisible = ref(false);
const selectedDate = ref("");
const selectedDayOfWeek = ref("");
const isDetailModalVisible = ref(false);
const detailLoading = ref(false);
const detailProcessing = ref(false);
const selectedBookingId = ref(null);
const selectedBookingDetail = ref(null);
const selectedEquipmentBookings = ref([]);
const equipmentDetailLoading = ref(false);
const equipmentProcessingId = ref(null);
const equipmentPendingCount = ref(0);
const equipmentReviewLoading = ref(false);
const equipmentReviewPage = ref(normalizeEquipmentBookingPage());
const isEquipmentDetailModalVisible = ref(false);
const selectedEquipmentBookingDetail = ref(null);
const isReviewStickyPinned = ref(false);
const venueListFilters = createReviewFilterState();
const equipmentFilters = createReviewFilterState();
const venueDateRangePickerRef = ref(null);
const venueDateRangePopoverRef = ref(null);
const equipmentDateRangePickerRef = ref(null);
const equipmentDateRangePopoverRef = ref(null);

const initialReviewRouteState = parseReviewRouteQuery(route.query);
if (initialReviewRouteState.activeReviewMode) {
  activeReviewMode.value = initialReviewRouteState.activeReviewMode;
}
if (initialReviewRouteState.equipmentKeyword) {
  equipmentFilters.keyword = initialReviewRouteState.equipmentKeyword;
}
if (initialReviewRouteState.equipmentStatus !== null) {
  equipmentSelectedStatus.value = initialReviewRouteState.equipmentStatus;
}

const isReviewer = computed(() => authSession.isReviewer);
const isAllVenuesSelected = computed(() => selectedVenueId.value === ALL_VENUES_VALUE);
const canNavigateToVenueBooking = computed(() => {
  if (isAllVenuesSelected.value) return venues.value.length > 0;

  return Boolean(selectedVenueId.value);
});

const selectedVenueName = computed(() => {
  if (isAllVenuesSelected.value) return "\u5168\u90e8\u5834\u5730";

  return (
    venues.value.find((venue) => String(venue.id) === String(selectedVenueId.value))?.name ||
    "\u672a\u77e5\u5834\u5730"
  );
});

const bookingRouteLabel = computed(() => {
  if (isAllVenuesSelected.value) return "\u524d\u5f80\u5834\u5730\u9810\u7d04";

  return `\u524d\u5f80 ${selectedVenueName.value} \u9810\u7d04`;
});
const getNavbarHeight = () => {
  const navbarHeight = document.querySelector(".navbar")?.getBoundingClientRect().height;
  if (navbarHeight) return navbarHeight;

  const headerHeight = Number.parseFloat(getComputedStyle(document.documentElement).getPropertyValue("--header-height")) || 0;
  return headerHeight + 18;
};

const updateReviewStickyState = () => {
  const reviewPage = reviewPageRef.value;
  const stickyStack = reviewStickyStackRef.value;

  if (!reviewPage || !stickyStack) {
    reviewPage?.style.setProperty("--review-sticky-stack-height", "0px");
    isReviewStickyPinned.value = false;
    return;
  }

  const stickyStackRect = stickyStack.getBoundingClientRect();
  const stickyStackMarginBottom = Number.parseFloat(window.getComputedStyle(stickyStack).marginBottom) || 0;

  reviewPage.style.setProperty(
    "--review-sticky-stack-height",
    `${stickyStackRect.height + stickyStackMarginBottom}px`,
  );
  isReviewStickyPinned.value = stickyStackRect.top <= getNavbarHeight() + 1;
};

let reviewStickyResizeObserver = null;

const reconnectReviewStickyObserver = () => {
  reviewStickyResizeObserver?.disconnect();
  reviewStickyResizeObserver = null;

  if (typeof ResizeObserver === "undefined" || !reviewStickyStackRef.value) return;

  reviewStickyResizeObserver = new ResizeObserver(() => {
    updateReviewStickyState();
  });
  reviewStickyResizeObserver.observe(reviewStickyStackRef.value);
};

let calendarTitleElement = null;

const formatEquipmentBorrowDateMeta = (value) => {
  if (!value) return "\u672a\u63d0\u4f9b\u501f\u7528\u65e5\u671f";

  const date = new Date(`${value}T00:00:00`);
  if (Number.isNaN(date.getTime())) return value;

  return new Intl.DateTimeFormat("zh-TW", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    weekday: "short",
  }).format(date);
};

const parseDateString = (value) => {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value || "")) return null;

  const [year, month, day] = value.split("-").map(Number);
  const date = new Date(year, month - 1, day);

  if (
    date.getFullYear() !== year
    || date.getMonth() !== month - 1
    || date.getDate() !== day
  ) {
    return null;
  }

  return date;
};

const getCurrentMonthDateRange = () => {
  const today = new Date();
  const startDate = new Date(today.getFullYear(), today.getMonth(), 1);
  const endDate = new Date(today.getFullYear(), today.getMonth() + 1, 0);

  return {
    startDate: formatDateKey(startDate),
    endDate: formatDateKey(endDate),
  };
};

currentCalendarVisibleRange.value = getCurrentMonthDateRange();

const formatDatePickerLabel = (value) => {
  const date = parseDateString(value);

  if (!date) return "\u672a\u9078\u64c7\u65e5\u671f";

  return new Intl.DateTimeFormat("zh-TW", {
    month: "numeric",
    day: "numeric",
    weekday: "short",
  }).format(date);
};

const getReviewFilterRangeLabel = (filters) => {
  if (!filters.startDate && !filters.endDate) {
    return "\u672a\u9078\u64c7";
  }

  if (!filters.startDate && filters.endDate) {
    return `\u81f3 ${formatDatePickerLabel(filters.endDate)}`;
  }

  if (filters.startDate && !filters.endDate) {
    return `${formatDatePickerLabel(filters.startDate)} \u4e4b\u5f8c`;
  }

  if (filters.startDate === filters.endDate) {
    return formatDatePickerLabel(filters.startDate);
  }

  return `${formatDatePickerLabel(filters.startDate)} - ${formatDatePickerLabel(filters.endDate)}`;
};

const getReviewFilterRangeHint = (filters) => {
  if (!filters.startDate && !filters.endDate) {
    return "\u672a\u8a2d\u5b9a\u8d77\u8fc4\uff0c\u5c07\u986f\u793a\u9810\u8a2d\u8cc7\u6599\u7bc4\u570d\u3002";
  }

  if (!filters.startDate && filters.endDate) {
    return "\u5c07\u5305\u542b\u8a72\u65e5\u671f\u4e4b\u524d\u7684\u6240\u6709\u8cc7\u6599\u3002";
  }

  if (filters.startDate && !filters.endDate) {
    return "\u5c07\u5305\u542b\u8a72\u65e5\u671f\u4e4b\u5f8c\u7684\u6240\u6709\u8cc7\u6599\u3002";
  }

  return "\u5c07\u53ea\u986f\u793a\u5340\u9593\u5167\u7684\u8cc7\u6599\u3002";
};

const updateReviewFilterStartDate = (filters, dateString) => {
  const normalizedDate = parseDateString(dateString) ? dateString : "";
  filters.startDate = normalizedDate;

  if (normalizedDate && filters.endDate && filters.endDate < normalizedDate) {
    filters.endDate = "";
  }
};

const updateReviewFilterEndDate = (filters, dateString) => {
  const normalizedDate = parseDateString(dateString) ? dateString : "";
  filters.endDate = normalizedDate;

  if (normalizedDate && filters.startDate && normalizedDate < filters.startDate) {
    filters.startDate = "";
  }
};

const clearReviewDateRange = (filters) => {
  filters.startDate = "";
  filters.endDate = "";
};

const toggleReviewDatePicker = (filters) => {
  filters.datePickerOpen = !filters.datePickerOpen;
};

const closeReviewDatePicker = (filters) => {
  filters.datePickerOpen = false;
};

const handleReviewDatePickerOutsideClick = (event, filters, pickerRef, popoverRef) => {
  if (!filters.datePickerOpen || !pickerRef.value) return;

  const clickedTrigger = pickerRef.value.contains(event.target);
  const clickedPopover = popoverRef.value?.contains(event.target);

  if (!clickedTrigger && !clickedPopover) {
    filters.datePickerOpen = false;
  }
};

const formatMonthPickerValue = (date) => {
  if (!(date instanceof Date) || Number.isNaN(date.getTime())) return "";

  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");

  return `${year}-${month}`;
};

const getCalendarMonthPickerValue = () => {
  const currentStart = calendarRef.value?.getApi()?.view?.currentStart;

  return formatMonthPickerValue(currentStart ? new Date(currentStart) : new Date());
};

const closeMonthPicker = () => {
  isMonthPickerOpen.value = false;
};

const handleMonthPickerOutsideClick = (event) => {
  if (!isMonthPickerOpen.value) return;

  const target = event.target;
  const pickerElement = monthPickerRef.value?.closest(".month-picker-popover");

  if (pickerElement?.contains(target) || calendarTitleElement?.contains(target)) return;

  closeMonthPicker();
};

const handleDocumentClick = (event) => {
  handleMonthPickerOutsideClick(event);
  handleReviewDatePickerOutsideClick(
    event,
    venueListFilters,
    venueDateRangePickerRef,
    venueDateRangePopoverRef,
  );
  handleReviewDatePickerOutsideClick(
    event,
    equipmentFilters,
    equipmentDateRangePickerRef,
    equipmentDateRangePopoverRef,
  );
};

const openMonthPicker = async () => {
  monthPickerValue.value = getCalendarMonthPickerValue();
  isMonthPickerOpen.value = true;

  await nextTick();
  monthPickerRef.value?.focus();
};

const goToSelectedMonth = () => {
  if (!monthPickerValue.value) return;

  const targetDate = new Date(`${monthPickerValue.value}-01T00:00:00`);

  if (Number.isNaN(targetDate.getTime())) return;

  closeTransientUi();
  calendarRef.value?.getApi()?.gotoDate(targetDate);
  closeMonthPicker();
};

const clearCalendarTitleInteraction = () => {
  if (!calendarTitleElement) return;

  calendarTitleElement.onclick = null;
  calendarTitleElement.onkeydown = null;
  calendarTitleElement = null;
};

const enhanceCalendarTitleInteraction = async () => {
  await nextTick();

  const titleElement = calendarRef.value?.$el?.querySelector(".fc-toolbar-title");

  if (!titleElement || titleElement === calendarTitleElement) return;

  clearCalendarTitleInteraction();

  titleElement.setAttribute("role", "button");
  titleElement.setAttribute("tabindex", "0");
  titleElement.setAttribute("title", "\u9078\u64c7\u6708\u4efd");
  titleElement.classList.add("is-month-picker-trigger");
  titleElement.onclick = openMonthPicker;
  titleElement.onkeydown = (event) => {
    if (event.key !== "Enter" && event.key !== " ") return;

    event.preventDefault();
    void openMonthPicker();
  };

  calendarTitleElement = titleElement;
};

const getReviewStatusText = (status) => {
  const statusMeta = getBookingStatusMeta(status);
  return statusMeta.text || "";
};

const getReviewBookingDateValue = (booking) => String(booking?.bookingDate || "");

const getReviewBookingEarliestSlot = (booking) => {
  const validSlots = Array.isArray(booking?.slots)
    ? booking.slots.map(Number).filter(Number.isFinite)
    : [];

  return validSlots.length ? Math.min(...validSlots) : Number.POSITIVE_INFINITY;
};

const getReviewBookingIdValue = (booking) => {
  const numericId = Number(booking?.id);
  if (Number.isFinite(numericId)) return numericId;

  return Number.POSITIVE_INFINITY;
};

const compareReviewBookingsByDate = (left, right, direction = "asc") => {
  const dateComparison = direction === "desc"
    ? getReviewBookingDateValue(right).localeCompare(getReviewBookingDateValue(left))
    : getReviewBookingDateValue(left).localeCompare(getReviewBookingDateValue(right));

  if (dateComparison !== 0) return dateComparison;

  const timeComparison = direction === "desc"
    ? getReviewBookingEarliestSlot(right) - getReviewBookingEarliestSlot(left)
    : getReviewBookingEarliestSlot(left) - getReviewBookingEarliestSlot(right);

  if (timeComparison !== 0) return timeComparison;

  return getReviewBookingIdValue(left) - getReviewBookingIdValue(right);
};

const compareReviewBookings = (left, right) => {
  if (selectedSort.value === "date-desc") {
    return compareReviewBookingsByDate(left, right, "desc");
  }

  if (selectedSort.value === "id-desc") {
    const idComparison = getReviewBookingIdValue(right) - getReviewBookingIdValue(left);

    if (idComparison !== 0) return idComparison;

    return compareReviewBookingsByDate(left, right, "desc");
  }

  if (selectedSort.value === "id-asc") {
    const idComparison = getReviewBookingIdValue(left) - getReviewBookingIdValue(right);

    if (idComparison !== 0) return idComparison;

    return compareReviewBookingsByDate(left, right, "asc");
  }

  return compareReviewBookingsByDate(left, right, "asc");
};

const getSortedReviewBookings = (bookings) => {
  return [...bookings].sort(compareReviewBookings);
};

const getEquipmentBookingDateValue = (booking) => String(booking?.borrowDate || "");

const getEquipmentBookingEarliestSlot = (booking) => {
  const validSlots = Array.isArray(booking?.slots)
    ? booking.slots.map(Number).filter(Number.isFinite)
    : [];

  return validSlots.length ? Math.min(...validSlots) : Number.POSITIVE_INFINITY;
};

const getEquipmentBookingIdValue = (booking) => {
  const numericId = Number(booking?.id);
  if (Number.isFinite(numericId)) return numericId;

  return Number.POSITIVE_INFINITY;
};

const compareEquipmentReviewBookingsByDate = (left, right, direction = "asc") => {
  const dateComparison = direction === "desc"
    ? getEquipmentBookingDateValue(right).localeCompare(getEquipmentBookingDateValue(left))
    : getEquipmentBookingDateValue(left).localeCompare(getEquipmentBookingDateValue(right));

  if (dateComparison !== 0) return dateComparison;

  const timeComparison = direction === "desc"
    ? getEquipmentBookingEarliestSlot(right) - getEquipmentBookingEarliestSlot(left)
    : getEquipmentBookingEarliestSlot(left) - getEquipmentBookingEarliestSlot(right);

  if (timeComparison !== 0) return timeComparison;

  return getEquipmentBookingIdValue(left) - getEquipmentBookingIdValue(right);
};

const compareEquipmentReviewBookings = (left, right) => {
  if (equipmentSelectedSort.value === "date-desc") {
    return compareEquipmentReviewBookingsByDate(left, right, "desc");
  }

  if (equipmentSelectedSort.value === "id-desc") {
    const idComparison = getEquipmentBookingIdValue(right) - getEquipmentBookingIdValue(left);

    if (idComparison !== 0) return idComparison;

    return compareEquipmentReviewBookingsByDate(left, right, "desc");
  }

  if (equipmentSelectedSort.value === "id-asc") {
    const idComparison = getEquipmentBookingIdValue(left) - getEquipmentBookingIdValue(right);

    if (idComparison !== 0) return idComparison;

    return compareEquipmentReviewBookingsByDate(left, right, "asc");
  }

  return compareEquipmentReviewBookingsByDate(left, right, "asc");
};

const getSortedEquipmentReviewBookings = (bookings) => {
  return [...bookings].sort(compareEquipmentReviewBookings);
};

const venueListDisplayBookings = computed(() => {
  return venueListBookingsSource.value.map((booking) => {
    const parsedContact = parseContactInfo(booking.contactInfo);
    const statusMeta = getBookingStatusMeta(booking.status);

    return {
      ...booking,
      venueName: booking.venueName || selectedVenueName.value || "\u672a\u77e5\u5834\u5730",
      purpose: booking.purpose || "",
      contactName: parsedContact.name || "\u672a\u63d0\u4f9b\u7533\u8acb\u4eba",
      participantCount: booking.pCount || 0,
      timeRange: formatSlotGroupsAsTimeRange(booking.slots),
      statusText: getReviewStatusText(booking.status),
      statusClass: statusMeta.className,
    };
  });
});

const venueListKeywordDateFilteredBookings = computed(() => {
  return filterVenueReviewList(venueListDisplayBookings.value, venueListFilters);
});

const calendarStatusCounts = computed(() => countReviewStatuses(allMonthlyBookings.value));

const venueListStatusCounts = computed(() => {
  return countReviewStatuses(venueListKeywordDateFilteredBookings.value);
});

const statusCounts = computed(() => {
  return activeViewMode.value === "list"
    ? venueListStatusCounts.value
    : calendarStatusCounts.value;
});

const venuePendingCount = computed(() => statusCounts.value.pending);
const venueListHasActiveFilters = computed(() => hasActiveReviewFilters(venueListFilters));
const equipmentHasActiveFilters = computed(() => hasActiveReviewFilters(equipmentFilters));

const clearVenueListFilters = () => {
  venueListFilters.keyword = "";
  clearReviewDateRange(venueListFilters);
  closeReviewDatePicker(venueListFilters);
};

const clearEquipmentFilters = () => {
  equipmentFilters.keyword = "";
  clearReviewDateRange(equipmentFilters);
  closeReviewDatePicker(equipmentFilters);
};

const statusFilterOptions = computed(() => [
  {
    key: "all",
    label: "\u5168\u90e8\u7533\u8acb",
    helper: "\u986f\u793a\u76ee\u524d\u689d\u4ef6\u4e0b\u7684\u6240\u6709\u5834\u5730\u5be9\u6838\u6848\u4ef6\u3002",
    value: statusCounts.value.all,
    className: "is-all",
    icon: ClipboardList,
    statusValue: "",
  },
  {
    key: "pending",
    label: "\u5f85\u5be9\u6838",
    helper: "\u5c1a\u672a\u8655\u7406\u7684\u5834\u5730\u9810\u7d04\u7533\u8acb\u3002",
    value: statusCounts.value.pending,
    className: "is-pending",
    icon: Clock3,
    statusValue: "1",
  },
  {
    key: "approved",
    label: "\u5df2\u901a\u904e",
    helper: "\u5df2\u901a\u904e\u7684\u5834\u5730\u9810\u7d04\u7533\u8acb\u3002",
    value: statusCounts.value.approved,
    className: "is-approved",
    icon: BadgeCheck,
    statusValue: "2",
  },
  {
    key: "rejected",
    label: "\u5df2\u9000\u4ef6",
    helper: "\u5df2\u9000\u56de\u7684\u5834\u5730\u9810\u7d04\u7533\u8acb\u3002",
    value: statusCounts.value.rejected,
    className: "is-rejected",
    icon: XCircle,
    statusValue: "3",
  },
]);

const equipmentKeywordDateFilteredItems = computed(() => {
  return filterEquipmentReviewList(equipmentReviewPage.value.items, equipmentFilters);
});

const equipmentStatusCounts = computed(() => {
  return countReviewStatuses(equipmentKeywordDateFilteredItems.value);
});

const equipmentStatusFilterOptions = computed(() => [
  {
    key: "all",
    label: "\u5168\u90e8\u7533\u8acb",
    helper: "\u986f\u793a\u76ee\u524d\u689d\u4ef6\u4e0b\u7684\u6240\u6709\u8a2d\u5099\u5be9\u6838\u6848\u4ef6\u3002",
    value: equipmentStatusCounts.value.all,
    className: "is-all",
    icon: ClipboardList,
    statusValue: "",
  },
  {
    key: "pending",
    label: "\u5f85\u5be9\u6838",
    helper: "\u5c1a\u672a\u8655\u7406\u7684\u8a2d\u5099\u501f\u7528\u7533\u8acb\u3002",
    value: equipmentStatusCounts.value.pending,
    className: "is-pending",
    icon: Clock3,
    statusValue: "1",
  },
  {
    key: "approved",
    label: "\u5df2\u901a\u904e",
    helper: "\u5df2\u901a\u904e\u7684\u8a2d\u5099\u501f\u7528\u7533\u8acb\u3002",
    value: equipmentStatusCounts.value.approved,
    className: "is-approved",
    icon: BadgeCheck,
    statusValue: "2",
  },
  {
    key: "rejected",
    label: "\u5df2\u9000\u4ef6",
    helper: "\u5df2\u9000\u56de\u7684\u8a2d\u5099\u501f\u7528\u7533\u8acb\u3002",
    value: equipmentStatusCounts.value.rejected,
    className: "is-rejected",
    icon: XCircle,
    statusValue: "3",
  },
]);

const getReviewEquipmentStatusMeta = (status) => {
  const meta = getEquipmentBookingStatusMeta(status);
  return {
    ...meta,
    text: meta.text || "",
  };
};

const filteredEquipmentReviewItems = computed(() => {
  const filteredBookings = equipmentSelectedStatus.value === ""
    ? equipmentKeywordDateFilteredItems.value
    : equipmentKeywordDateFilteredItems.value.filter((booking) => booking.status === Number(equipmentSelectedStatus.value));

  return getSortedEquipmentReviewBookings(filteredBookings);
});

const sortedMonthlyBookings = computed(() => getSortedReviewBookings(monthlyBookings.value));

const selectedDayBookings = computed(() => {
  if (!selectedDate.value) return [];

  return sortedMonthlyBookings.value
    .filter((booking) => booking.bookingDate === selectedDate.value)
    .map((booking) => {
      const parsedContact = parseContactInfo(booking.contactInfo);
      const statusMeta = getBookingStatusMeta(booking.status);

      return {
        id: booking.id,
        purpose: booking.purpose || "",
        venueName: booking.venueName || selectedVenueName.value || "\u672a\u77e5\u5834\u5730",
        contactName: parsedContact.name || "\u672a\u63d0\u4f9b\u7533\u8acb\u4eba",
        participantCount: booking.pCount || 0,
        timeRange: formatSlotGroupsAsTimeRange(booking.slots),
        statusText: getReviewStatusText(booking.status),
        statusClass: statusMeta.className,
      };
    });
});

const reviewListBookings = computed(() => {
  const filteredBookings = selectedStatus.value === ""
    ? venueListKeywordDateFilteredBookings.value
    : venueListKeywordDateFilteredBookings.value.filter((booking) => booking.status === Number(selectedStatus.value));

  return getSortedReviewBookings(filteredBookings);
});

const createReviewStatusOptions = (counts, scopeLabel = "\u7533\u8acb") => [
  {
    key: "all",
    label: "\u5168\u90e8\u7533\u8acb",
    helper: `\u986f\u793a\u76ee\u524d\u689d\u4ef6\u4e0b\u7684\u6240\u6709${scopeLabel}\u6848\u4ef6\u3002`,
    value: counts.all,
    className: "is-all",
    icon: ClipboardList,
    statusValue: "",
  },
  {
    key: "pending",
    label: "\u5f85\u5be9\u6838",
    helper: `\u5c1a\u672a\u8655\u7406\u7684${scopeLabel}\u6848\u4ef6\u3002`,
    value: counts.pending,
    className: "is-pending",
    icon: Clock3,
    statusValue: "1",
  },
  {
    key: "approved",
    label: "\u5df2\u901a\u904e",
    helper: `\u5df2\u901a\u904e\u7684${scopeLabel}\u6848\u4ef6\u3002`,
    value: counts.approved,
    className: "is-approved",
    icon: BadgeCheck,
    statusValue: "2",
  },
  {
    key: "rejected",
    label: "\u5df2\u9000\u4ef6",
    helper: `\u5df2\u9000\u56de\u7684${scopeLabel}\u6848\u4ef6\u3002`,
    value: counts.rejected,
    className: "is-rejected",
    icon: XCircle,
    statusValue: "3",
  },
];

const venueReviewStatusTabs = computed(() => createReviewStatusOptions(statusCounts.value, "\u5834\u5730\u7533\u8acb"));
const equipmentReviewStatusTabs = computed(() => createReviewStatusOptions(equipmentStatusCounts.value, "\u8a2d\u5099\u7533\u8acb"));

const getReviewEquipmentStatusDisplayMeta = (status) => {
  const meta = getEquipmentBookingStatusMeta(status);
  return {
    ...meta,
    text: meta.text || "",
  };
};

const getReviewTotalPages = (items) => {
  if (!items.length) return 1;
  return Math.ceil(items.length / REVIEW_LIST_PAGE_SIZE);
};

const getVisibleReviewPageNumbers = (currentPage, totalPages) => {
  const visibleCount = Math.min(totalPages, 5);
  let startPage = currentPage - Math.floor(visibleCount / 2);

  if (startPage < 1) startPage = 1;
  if (startPage + visibleCount - 1 > totalPages) {
    startPage = totalPages - visibleCount + 1;
  }

  return Array.from({ length: visibleCount }, (_, index) => startPage + index);
};

const getPaginatedReviewItems = (items, pageNo) => {
  const startIndex = (pageNo - 1) * REVIEW_LIST_PAGE_SIZE;
  return items.slice(startIndex, startIndex + REVIEW_LIST_PAGE_SIZE);
};

const venueReviewTotalPages = computed(() => getReviewTotalPages(reviewListBookings.value));
const visibleVenueReviewPageNumbers = computed(() => getVisibleReviewPageNumbers(
  venueReviewCurrentPage.value,
  venueReviewTotalPages.value,
));
const canGoPreviousVenueReviewPage = computed(() => venueReviewCurrentPage.value > 1);
const canGoNextVenueReviewPage = computed(() => venueReviewCurrentPage.value < venueReviewTotalPages.value);
const paginatedReviewListBookings = computed(() => getPaginatedReviewItems(
  reviewListBookings.value,
  venueReviewCurrentPage.value,
));
const venueReviewPaginationStartIndex = computed(() => {
  if (reviewListBookings.value.length === 0) return 0;
  return (venueReviewCurrentPage.value - 1) * REVIEW_LIST_PAGE_SIZE + 1;
});
const venueReviewPaginationEndIndex = computed(() => Math.min(
  venueReviewCurrentPage.value * REVIEW_LIST_PAGE_SIZE,
  reviewListBookings.value.length,
));

const equipmentReviewTotalPages = computed(() => getReviewTotalPages(filteredEquipmentReviewItems.value));
const visibleEquipmentReviewPageNumbers = computed(() => getVisibleReviewPageNumbers(
  equipmentReviewCurrentPage.value,
  equipmentReviewTotalPages.value,
));
const canGoPreviousEquipmentReviewPage = computed(() => equipmentReviewCurrentPage.value > 1);
const canGoNextEquipmentReviewPage = computed(() => equipmentReviewCurrentPage.value < equipmentReviewTotalPages.value);
const paginatedEquipmentReviewItems = computed(() => getPaginatedReviewItems(
  filteredEquipmentReviewItems.value,
  equipmentReviewCurrentPage.value,
));
const equipmentReviewPaginationStartIndex = computed(() => {
  if (filteredEquipmentReviewItems.value.length === 0) return 0;
  return (equipmentReviewCurrentPage.value - 1) * REVIEW_LIST_PAGE_SIZE + 1;
});
const equipmentReviewPaginationEndIndex = computed(() => Math.min(
  equipmentReviewCurrentPage.value * REVIEW_LIST_PAGE_SIZE,
  filteredEquipmentReviewItems.value.length,
));

const setVenueReviewPage = (pageNo) => {
  venueReviewCurrentPage.value = Math.min(Math.max(pageNo, 1), venueReviewTotalPages.value);
};

const goToPreviousVenueReviewPage = () => {
  setVenueReviewPage(venueReviewCurrentPage.value - 1);
};

const goToNextVenueReviewPage = () => {
  setVenueReviewPage(venueReviewCurrentPage.value + 1);
};

const setEquipmentReviewPage = (pageNo) => {
  equipmentReviewCurrentPage.value = Math.min(Math.max(pageNo, 1), equipmentReviewTotalPages.value);
};

const goToPreviousEquipmentReviewPage = () => {
  setEquipmentReviewPage(equipmentReviewCurrentPage.value - 1);
};

const goToNextEquipmentReviewPage = () => {
  setEquipmentReviewPage(equipmentReviewCurrentPage.value + 1);
};

const getEquipmentReviewActions = (equipmentBooking) => {
  // Reviewers can now correct an equipment decision after approval or rejection.
  // The UI exposes only review-owned states here; user withdrawal remains a
  // borrower-side action and is intentionally not offered from this workbench.
  switch (equipmentBooking?.status) {
    case 1:
      return [
        { key: "reject", label: "\u9000\u4ef6", icon: XCircle, buttonClass: "btn-danger", status: 3 },
        { key: "approve", label: "\u901a\u904e", icon: Check, buttonClass: "btn-primary", status: 2 },
      ];
    case 2:
      return [
        { key: "reject-approved", label: "\u6539\u70ba\u9000\u4ef6", icon: XCircle, buttonClass: "btn-danger", status: 3 },
      ];
    case 3:
      return [
        { key: "pending-rejected", label: "\u6539\u70ba\u5f85\u5be9\u6838", icon: RotateCcw, buttonClass: "btn-secondary-alt", status: 1 },
        { key: "approve-rejected", label: "\u6539\u70ba\u901a\u904e", icon: Check, buttonClass: "btn-primary", status: 2 },
      ];
    default:
      return [];
  }
};

const renderEventContent = (arg) => {
  const wrapper = document.createElement("div");
  wrapper.className = "calendar-event-content";

  const status = document.createElement("span");
  status.className = `calendar-event-status ${arg.event.extendedProps.statusClass}`;
  status.textContent = arg.event.extendedProps.statusLabel;

  const time = document.createElement("span");
  time.className = "calendar-event-time";
  time.textContent = arg.event.extendedProps.timeLabel;

  const purpose = document.createElement("span");
  purpose.className = "calendar-event-purpose";
  purpose.textContent = arg.event.extendedProps.purposeLabel;

  wrapper.append(status, time, purpose);

  return {
    domNodes: [wrapper],
  };
};

const renderDayCellContent = (arg) => {
  const count = getDailyEventCount(events.value, arg.date);

  return {
    html: `
      <span class="calendar-day-number">${arg.dayNumberText}</span>
      ${count > 0 ? `<span class="calendar-day-count">${count}</span>` : ""}
    `,
  };
};

const calendarOptions = ref({
  plugins: [dayGridPlugin, interactionPlugin],
  initialView: "dayGridMonth",
  headerToolbar: {
    left: "",
    center: "prev title next",
    right: "",
  },
  locale: "zh-tw",
  firstDay: 0,
  height: "auto",
  dayMaxEvents: 3,
  eventTimeFormat: {
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  },
  dayCellContent: renderDayCellContent,
  eventContent: renderEventContent,
  moreLinkContent: (arg) => renderMoreLinkContent(arg, "\u7b46"),
  moreLinkClick: () => {},
  events,
  datesSet: async (arg) => {
    if (!selectedVenueId.value) return;
    await loadEvents(arg.view);
  },
  dateClick: (info) => {
    const dateStr = info.dateStr.split("T")[0];
    openDayModal(dateStr);
  },
  eventClick: async (info) => {
    const bookingId = info.event.extendedProps.bookingId;
    await openBookingDetail(bookingId);
  },
});

const refreshCalendarLayout = async () => {
  await nextTick();
  calendarRef.value?.getApi()?.updateSize();
  await enhanceCalendarTitleInteraction();
};

const getVisibleDateRangeFromView = (view) => {
  const startDate = view.activeStart || view.currentStart;
  const endDate = view.activeEnd || view.currentEnd;

  if (!startDate || !endDate) {
    const fallbackStart = new Date(view.currentStart);
    const fallbackEnd = new Date(fallbackStart);
    fallbackEnd.setMonth(fallbackEnd.getMonth() + 1);
    fallbackEnd.setDate(fallbackEnd.getDate() - 1);

    return {
      startDate: formatDateKey(fallbackStart),
      endDate: formatDateKey(fallbackEnd),
    };
  }

  const endInclusive = new Date(endDate);
  endInclusive.setDate(endInclusive.getDate() - 1);

  return {
    startDate: formatDateKey(startDate),
    endDate: formatDateKey(endInclusive),
  };
};

const mapBookingsToEvents = (bookings) => {
  const mappedEvents = [];

  bookings.forEach((booking) => {
    const slotGroups = groupContiguousSlots(booking.slots);
    const statusMeta = getBookingStatusMeta(booking.status);
    const purposeLabel = booking.purpose?.trim() || reviewCopy.noPurpose;
    const displayPurpose = isAllVenuesSelected.value
      ? `${booking.venueName || "\u672a\u63d0\u4f9b\u5834\u5730"} | ${purposeLabel}`
      : purposeLabel;

    slotGroups.forEach((group) => {
      const timeRange = convertSlotsToTimeRange(booking.bookingDate, group);

      if (!timeRange) return;

      mappedEvents.push({
        title: `${getReviewStatusText(booking.status)}｜${purposeLabel}`,
        start: timeRange.start,
        end: timeRange.end,
        display: "block",
        extendedProps: {
          bookingId: booking.id,
          booking,
          statusLabel: getReviewStatusText(booking.status),
          statusClass: statusMeta.className,
          timeLabel: formatSlotsAsTimeRange(group),
          purposeLabel: displayPurpose,
          fullTimeLabel: formatSlotGroupsAsTimeRange(booking.slots),
        },
        ...getReviewEventColorConfig(booking.status),
      });
    });
  });

  return mappedEvents;
};

const fetchReviewsForVenue = (venueId, query) => {
  return fetchPendingReviews({
    ...query,
    venueId,
  });
};

const fetchReviewsForSelectedVenue = async (query) => {
  if (!isAllVenuesSelected.value) {
    return fetchReviewsForVenue(selectedVenueId.value, query);
  }

  const bookingsByVenue = await Promise.all(
    venues.value.map((venue) => fetchReviewsForVenue(venue.id, query)),
  );
  const seenBookingIds = new Set();

  return bookingsByVenue.flat().filter((booking) => {
    if (seenBookingIds.has(booking.id)) return false;

    seenBookingIds.add(booking.id);
    return true;
  });
};

const applyVenueCalendarStatusFilter = () => {
  const filteredBookings = selectedStatus.value === ""
    ? allMonthlyBookings.value
    : allMonthlyBookings.value.filter((booking) => booking.status === Number(selectedStatus.value));

  monthlyBookings.value = filteredBookings;
  events.value = mapBookingsToEvents(getSortedReviewBookings(filteredBookings));
};

const getVenueListQueryRange = () => {
  if (venueListFilters.startDate || venueListFilters.endDate) {
    return {
      startDate: venueListFilters.startDate || null,
      endDate: venueListFilters.endDate || null,
    };
  }

  return {
    startDate: currentCalendarVisibleRange.value.startDate || null,
    endDate: currentCalendarVisibleRange.value.endDate || null,
  };
};

const loadVenueListBookings = async () => {
  isFetchingEvents.value = true;

  try {
    const bookings = await fetchReviewsForSelectedVenue(getVenueListQueryRange());
    venueListBookingsSource.value = Array.isArray(bookings) ? bookings : [];
  } catch (loadError) {
    venueListBookingsSource.value = [];
    error(loadError.message || "取得場地審核列表失敗。");
  } finally {
    isFetchingEvents.value = false;
    pageLoading.value = false;
  }
};

const loadEvents = async (view) => {
  isFetchingEvents.value = true;

  try {
    const visibleDateRange = getVisibleDateRangeFromView(view);
    currentCalendarVisibleRange.value = visibleDateRange;
    const baseQuery = {
      startDate: visibleDateRange.startDate,
      endDate: visibleDateRange.endDate,
    };

    const allBookings = await fetchReviewsForSelectedVenue(baseQuery);
    allMonthlyBookings.value = allBookings;
    applyVenueCalendarStatusFilter();
  } catch (loadError) {
    allMonthlyBookings.value = [];
    monthlyBookings.value = [];
    events.value = [];
    error(loadError.message || "取得審核月曆失敗。");
  } finally {
    isFetchingEvents.value = false;
    pageLoading.value = false;

    if (activeViewMode.value === "calendar") {
      void refreshCalendarLayout();
    }
  }
};

const reloadCurrentView = async () => {
  const view = calendarRef.value?.getApi().view;

  if (view) {
    await loadEvents(view);
  }
};

const enrichEquipmentReviewItemsWithVenueBookingName = async (bookings) => {
  const relatedVenueBookingIds = [...new Set(
    bookings.map((booking) => booking.relatedVenueBookingId).filter(Boolean),
  )];

  if (relatedVenueBookingIds.length === 0) return bookings;

  const bookingTitles = Object.fromEntries(
    await Promise.all(
      relatedVenueBookingIds.map(async (bookingId) => {
        try {
          const bookingDetail = await fetchReviewBookingDetail(bookingId);
          return [bookingId, bookingDetail?.purpose?.trim() || reviewCopy.noPurpose];
        } catch (loadError) {
          console.error(`\u8f09\u5165\u95dc\u806f\u5834\u5730\u9810\u7d04 ${bookingId} \u6a19\u984c\u5931\u6557:`, loadError);
          return [bookingId, null];
        }
      }),
    ),
  );

  return bookings.map((booking) => {
    if (!booking.relatedVenueBookingId) return booking;

    const bookingTitle = bookingTitles[booking.relatedVenueBookingId] || null;

    return {
      ...booking,
      relatedVenueBookingTitle: bookingTitle,
    };
  });
};

const loadEquipmentPendingCount = async () => {
  try {
    const pendingPage = await queryEquipmentReviews({
      statusList: [1],
      pageNo: 1,
      pageSize: 1,
    });
    equipmentPendingCount.value = Number(pendingPage?.total) || 0;
  } catch (countError) {
    console.error("取得設備待審核數量失敗", countError);
    equipmentPendingCount.value = 0;
  }
};

const fetchAllEquipmentReviewItems = async (query = {}) => {
  const collectedItems = [];
  let pageNo = 1;
  let totalPages = 1;
  let total = 0;

  while (pageNo <= totalPages) {
    const page = normalizeEquipmentBookingPage(
      await queryEquipmentReviews({
        ...query,
        pageNo,
        pageSize: REVIEW_PAGE_SIZE,
      }),
    );

    total = page.total;
    totalPages = Math.max(page.totalPages, 1);
    collectedItems.push(...page.items);

    if (page.items.length === 0) break;

    pageNo += 1;
  }

  return normalizeEquipmentBookingPage({
    total: total || collectedItems.length,
    pageNo: 1,
    currentPage: 1,
    pageSize: Math.max(collectedItems.length, REVIEW_PAGE_SIZE),
    totalPages: collectedItems.length > 0 ? 1 : 0,
    hasNext: false,
    items: collectedItems,
  });
};

const loadEquipmentReviews = async () => {
  equipmentReviewLoading.value = true;

  try {
    // The equipment tab is a reviewer-facing list for every equipment request,
    // including standalone requests and requests linked to venue bookings.
    const equipmentReviewPageData = await fetchAllEquipmentReviewItems({
      startDate: equipmentFilters.startDate || null,
      endDate: equipmentFilters.endDate || null,
    });
    const enrichedItems = await enrichEquipmentReviewItemsWithVenueBookingName(equipmentReviewPageData.items);
    equipmentReviewPage.value = {
      ...equipmentReviewPageData,
      items: enrichedItems,
      data: enrichedItems,
    };
    await loadEquipmentPendingCount();
  } catch (standaloneError) {
    equipmentReviewPage.value = normalizeEquipmentBookingPage();
    error(standaloneError.message || "取得設備審核列表失敗。");
  } finally {
    equipmentReviewLoading.value = false;
  }
};

const openDayModal = (dateStr) => {
  selectedDate.value = dateStr;

  const targetDate = new Date(`${dateStr}T00:00:00`);
  selectedDayOfWeek.value = targetDate.toLocaleDateString("zh-TW", {
    weekday: "long",
  });

  isDayModalVisible.value = true;
};

const closeDayModal = () => {
  isDayModalVisible.value = false;
};

const closeTransientUi = () => {
  isDayModalVisible.value = false;
  isDetailModalVisible.value = false;
  detailLoading.value = false;
  detailProcessing.value = false;
  equipmentDetailLoading.value = false;
  isEquipmentDetailModalVisible.value = false;
  selectedBookingId.value = null;
  selectedBookingDetail.value = null;
  selectedEquipmentBookings.value = [];
  selectedEquipmentBookingDetail.value = null;
  selectedDate.value = "";
  selectedDayOfWeek.value = "";
};

const handleFilterChange = async () => {
  closeTransientUi();

  if (activeReviewMode.value === "venue" && activeViewMode.value === "list") {
    await loadVenueListBookings();
    return;
  }

  await reloadCurrentView();
};

const selectStatusFilter = async (statusValue) => {
  if (selectedStatus.value === statusValue) return;

  selectedStatus.value = statusValue;
  closeTransientUi();

  if (activeViewMode.value === "calendar") {
    applyVenueCalendarStatusFilter();
  }
};

const handleSortChange = () => {
  if (activeViewMode.value === "calendar") {
    events.value = mapBookingsToEvents(sortedMonthlyBookings.value);
  }
};

const selectEquipmentStatusFilter = (statusValue) => {
  if (equipmentSelectedStatus.value === statusValue) return;

  equipmentSelectedStatus.value = statusValue;
};

watch(activeViewMode, (nextMode) => {
  if (nextMode === "calendar") {
    applyVenueCalendarStatusFilter();
    void refreshCalendarLayout();
  } else {
    closeMonthPicker();
    closeTransientUi();
    void loadVenueListBookings();
  }
});

watch(activeReviewMode, (nextMode) => {
  // The two review modes have different data sources. Loading the equipment
  // review list lazily keeps the venue calendar path unchanged for reviewers
  // who only need to handle normal venue bookings.
  if (nextMode === "equipment") {
    closeTransientUi();
    void loadEquipmentReviews();
  } else {
    closeTransientUi();
    if (activeViewMode.value === "list") {
      void loadVenueListBookings();
    } else {
      void reloadCurrentView();
    }
    void loadEquipmentPendingCount();
  }

  void nextTick(updateReviewStickyState);
});

watch(
  [() => venueListFilters.startDate, () => venueListFilters.endDate],
  () => {
    if (activeReviewMode.value !== "venue" || activeViewMode.value !== "list") return;

    closeTransientUi();
    void loadVenueListBookings();
  },
);

watch(
  [() => equipmentFilters.startDate, () => equipmentFilters.endDate],
  () => {
    if (activeReviewMode.value !== "equipment") return;

    closeTransientUi();
    void loadEquipmentReviews();
  },
);

watch(
  [
    () => venueListFilters.keyword,
    () => venueListFilters.startDate,
    () => venueListFilters.endDate,
    () => selectedStatus.value,
    () => selectedVenueId.value,
    () => activeViewMode.value,
  ],
  () => {
    venueReviewCurrentPage.value = 1;
  },
);

watch(
  [
    () => equipmentFilters.keyword,
    () => equipmentFilters.startDate,
    () => equipmentFilters.endDate,
    () => equipmentSelectedStatus.value,
    () => activeReviewMode.value,
  ],
  () => {
    equipmentReviewCurrentPage.value = 1;
  },
);

watch(reviewListBookings, () => {
  if (venueReviewCurrentPage.value > venueReviewTotalPages.value) {
    venueReviewCurrentPage.value = venueReviewTotalPages.value;
  }
});

watch(filteredEquipmentReviewItems, () => {
  if (equipmentReviewCurrentPage.value > equipmentReviewTotalPages.value) {
    equipmentReviewCurrentPage.value = equipmentReviewTotalPages.value;
  }
});

watch(reviewStickyStackRef, async () => {
  await nextTick();
  reconnectReviewStickyObserver();
  updateReviewStickyState();
});

const navigateToVenueBooking = (dateStr) => {
  if (!canNavigateToVenueBooking.value) return;

  if (isAllVenuesSelected.value) {
    router.push({
      name: "VenueSelector",
      params: { unitId: REVIEW_UNIT_ID },
    });
    return;
  }

  const query = dateStr
    ? {
        create: "1",
        date: dateStr,
      }
    : {};

  router.push({
    name: "VenueCalendar",
    params: { venueId: String(selectedVenueId.value) },
    query,
  });
};

const navigateToEquipmentStatus = () => {
  if (!isReviewer.value) return;

  router.push({ name: "EquipmentStatus" });
};

const openBookingDetail = async (bookingId) => {
  isDayModalVisible.value = false;
  selectedBookingId.value = bookingId;
  selectedBookingDetail.value = null;
  selectedEquipmentBookings.value = [];
  detailLoading.value = true;
  equipmentDetailLoading.value = true;
  isDetailModalVisible.value = true;

  try {
    const [bookingDetail, equipmentBookings] = await Promise.all([
      fetchReviewBookingDetail(bookingId),
      getEquipmentReviewsByVenueBooking(bookingId),
    ]);
    selectedBookingDetail.value = bookingDetail;
    selectedEquipmentBookings.value = Array.isArray(equipmentBookings)
      ? equipmentBookings.map(normalizeEquipmentBooking)
      : [];
  } catch (detailError) {
    error(detailError.message || "取得申請詳情失敗。");
    closeDetailModal();
  } finally {
    detailLoading.value = false;
    equipmentDetailLoading.value = false;
  }
};

const closeDetailModal = () => {
  closeTransientUi();
};

const refreshAfterVenueReviewUpdate = async () => {
  if (activeReviewMode.value === "equipment") {
    await refreshEquipmentReviewState();
    return;
  }

  if (activeViewMode.value === "list") {
    await loadVenueListBookings();
    return;
  }

  await reloadCurrentView();
};

const handleApprove = async () => {
  if (!selectedBookingId.value) return;

  detailProcessing.value = true;
  let venueStatusUpdated = false;

  try {
    await approveReviewBooking(selectedBookingId.value);
    venueStatusUpdated = true;
    await syncLinkedEquipmentReviewStatus(2);
    success("\u5834\u5730\u9810\u7d04\u5df2\u901a\u904e\u3002");
    closeDetailModal();
    await refreshAfterVenueReviewUpdate();
  } catch (approveError) {
    if (venueStatusUpdated) {
      error(approveError.message || "\u5834\u5730\u9810\u7d04\u5df2\u66f4\u65b0\uff0c\u4f46\u540c\u6b65\u8a2d\u5099\u5be9\u6838\u72c0\u614b\u5931\u6557\u3002");
      closeDetailModal();
      await refreshAfterVenueReviewUpdate();
    } else {
      error(approveError.message || "\u901a\u904e\u7533\u8acb\u5931\u6557\u3002");
    }
  } finally {
    detailProcessing.value = false;
  }
};

const handleStatusUpdate = async (status) => {
  if (!selectedBookingId.value) return;

  detailProcessing.value = true;
  let venueStatusUpdated = false;

  try {
    await updateReviewBookingStatus(selectedBookingId.value, status);
    venueStatusUpdated = true;
    await syncLinkedEquipmentReviewStatus(status);
    success(`\u5df2\u5c07\u7533\u8acb\u72c0\u614b\u66f4\u65b0\u70ba${getReviewStatusText(status)}\u3002`);
    closeDetailModal();
    await refreshAfterVenueReviewUpdate();
  } catch (updateError) {
    if (venueStatusUpdated) {
      error(updateError.message || "\u5834\u5730\u9810\u7d04\u5df2\u66f4\u65b0\uff0c\u4f46\u540c\u6b65\u8a2d\u5099\u5be9\u6838\u72c0\u614b\u5931\u6557\u3002");
      closeDetailModal();
      await refreshAfterVenueReviewUpdate();
    } else {
      error(updateError.message || "\u66f4\u65b0\u7533\u8acb\u72c0\u614b\u5931\u6557\u3002");
    }
  } finally {
    detailProcessing.value = false;
  }
};

const isVenueEquipmentSyncStatus = (status) => {
  return [1, 2, 3].includes(Number(status));
};

const isReviewableEquipmentStatus = (status) => {
  return [1, 2, 3].includes(Number(status));
};

const syncLinkedEquipmentReviewStatus = async (status) => {
  if (!selectedBookingId.value || !isVenueEquipmentSyncStatus(status)) return;

  const equipmentBookings = await getEquipmentReviewsByVenueBooking(selectedBookingId.value);
  const normalizedBookings = Array.isArray(equipmentBookings)
    ? equipmentBookings.map(normalizeEquipmentBooking)
    : [];

  selectedEquipmentBookings.value = normalizedBookings;

  const targetStatus = Number(status);
  const syncTargets = normalizedBookings.filter((equipmentBooking) => {
    return (
      equipmentBooking.id &&
      isReviewableEquipmentStatus(equipmentBooking.status) &&
      Number(equipmentBooking.status) !== targetStatus
    );
  });

  await Promise.all(
    syncTargets.map((equipmentBooking) => updateEquipmentReviewStatus(equipmentBooking.id, targetStatus)),
  );
};

const refreshEquipmentReviewState = async () => {
  // Refresh only the data source that is currently visible. This avoids forcing
  // a full venue calendar reload when the reviewer is processing standalone
  // equipment requests, while still keeping modal equipment details current.
  await loadEquipmentPendingCount();

  if (activeReviewMode.value === "equipment") {
    await loadEquipmentReviews();
    return;
  }

  if (selectedBookingId.value && isDetailModalVisible.value) {
    const equipmentBookings = await getEquipmentReviewsByVenueBooking(selectedBookingId.value);
    selectedEquipmentBookings.value = Array.isArray(equipmentBookings)
      ? equipmentBookings.map(normalizeEquipmentBooking)
      : [];
  }
};

const openEquipmentDetail = (id) => {
  const booking = equipmentReviewPage.value.items.find((b) => b.id === id);
  if (booking) {
    selectedEquipmentBookingDetail.value = booking;
    isEquipmentDetailModalVisible.value = true;
  }
};

const openEquipmentReviewTarget = async (equipmentBooking) => {
  const target = getEquipmentReviewOpenTarget(equipmentBooking);
  if (!target) return;

  closeEquipmentDetailModal();

  if (target.type === "venue") {
    await openBookingDetail(target.id);
    return;
  }

  openEquipmentDetail(target.id);
};

const closeEquipmentDetailModal = () => {
  isEquipmentDetailModalVisible.value = false;
  selectedEquipmentBookingDetail.value = null;
};

const handleEquipmentDetailStatusUpdate = async (id, status) => {
  await handleEquipmentStatusUpdate(id, status);
  if (selectedEquipmentBookingDetail.value?.id === id) {
    const updated = equipmentReviewPage.value.items.find((b) => b.id === id);
    if (updated) {
      selectedEquipmentBookingDetail.value = updated;
    }
  }
};

const handleEquipmentStatusUpdate = async (equipmentBookingId, status) => {
  if (!equipmentBookingId) return;

  equipmentProcessingId.value = equipmentBookingId;

  try {
    await updateEquipmentReviewStatus(equipmentBookingId, status);
    success(`\u5df2\u5c07\u8a2d\u5099\u7533\u8acb\u72c0\u614b\u66f4\u65b0\u70ba${getEquipmentBookingStatusMeta(status).text}\u3002`);
    await refreshEquipmentReviewState();
  } catch (updateError) {
    error(updateError.message || "\u66f4\u65b0\u8a2d\u5099\u7533\u8acb\u72c0\u614b\u5931\u6557\u3002");
  } finally {
    equipmentProcessingId.value = null;
  }
};

onMounted(async () => {
  document.addEventListener("click", handleDocumentClick);
  window.addEventListener("scroll", updateReviewStickyState, { passive: true });
  window.addEventListener("resize", updateReviewStickyState);

  try {
    const fetchedVenues = await fetchVenuesByUnit(1);
    venues.value = fetchedVenues;
    pageLoading.value = false;

    if (selectedVenueId.value !== ALL_VENUES_VALUE && !fetchedVenues.some((venue) => venue.id === selectedVenueId.value)) {
      selectedVenueId.value = fetchedVenues[0]?.id || null;
    }

    if (fetchedVenues.length === 0) {
      pageLoading.value = false;
      error("\u76ee\u524d\u6c92\u6709\u53ef\u5be9\u6838\u7684\u5834\u5730\u3002");
    }
    await loadEquipmentPendingCount();
  } catch (venueError) {
    error(venueError.message || "載入可審核場地失敗。");
    pageLoading.value = false;
  }

  if (activeReviewMode.value === "equipment") {
    await loadEquipmentReviews();
  } else if (activeViewMode.value === "list" && activeReviewMode.value === "venue") {
    await loadVenueListBookings();
  }
  await nextTick();
  reconnectReviewStickyObserver();
  updateReviewStickyState();
});

onBeforeUnmount(() => {
  document.removeEventListener("click", handleDocumentClick);
  window.removeEventListener("scroll", updateReviewStickyState);
  window.removeEventListener("resize", updateReviewStickyState);
  reviewStickyResizeObserver?.disconnect();
  clearCalendarTitleInteraction();
});
</script>

<style lang="scss" scoped>
.review-page {
  --review-panel: #ffffff;
  --review-line: #d7dde5;
  --review-ink: #202936;
  --review-muted: #5f6b7a;
  --review-sticky-top: calc(var(--header-height) + 18px);
  --review-sticky-stack-height: 0px;
  --review-sticky-secondary-top: calc(var(--review-sticky-top) + var(--review-sticky-stack-height));

  .workbench-header {
    margin-bottom: 0.75rem;
    padding: 1.35rem 1.45rem;
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 1rem;
    border: 1px solid var(--review-line);
    border-radius: var(--radius);
    background: linear-gradient(180deg, #ffffff 0%, #f6f8fb 100%);
    box-shadow: var(--shadow-soft);
  }

  .header-copy {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    gap: 0.45rem;
    flex: 1 1 auto;

    h1,
    p {
      margin: 0;
    }

    h1 {
      color: var(--review-ink);
    }

    p:last-child {
      max-width: 42rem;
      color: var(--review-muted);
    }
  }

  .header-actions {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    gap: 0.85rem;
    margin-left: auto;
    align-self: stretch;
  }

  .header-actions > .review-mode-toggle {
    display: none;
  }

  .page-title {
    display: inline-flex;
    align-items: center;
    gap: 0.7rem;
  }

  .page-title-icon {
    flex-shrink: 0;
    color: var(--accent);
  }

  .btn-icon {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 1rem;
    height: 1rem;
    flex-shrink: 0;
  }

  .btn-danger {
    background: var(--danger);
    color: #ffffff;
  }

  .btn-danger:hover:not(:disabled) {
    box-shadow: 0 8px 18px rgba(196, 69, 69, 0.22);
  }

  .btn-secondary-alt {
    background: #f3f6fb;
    border-color: rgba(var(--blue-900-rgb), 0.12);
    color: var(--accent);
  }

  .header-eyebrow,
  .panel-kicker {
    margin: 0;
    color: #5b6675;
    font-size: var(--text-sm);
    font-weight: 800;
    text-transform: uppercase;
  }

  .mode-pill {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 0.45rem;
    min-height: 2rem;
    padding: 0.4rem 0.75rem;
    border: 1px solid rgba(32, 41, 54, 0.12);
    border-radius: 999px;
    background: #202936;
    color: #ffffff;
    font-size: var(--text-sm);
    font-weight: 800;
    line-height: 1;
  }

  .admin-equipment-btn {
    flex-shrink: 0;
    border: 1px solid rgba(36, 63, 107, 0.16);
    background: linear-gradient(135deg, rgba(243, 247, 252, 0.98), rgba(232, 240, 250, 0.96));
    color: #243f6b;
    box-shadow: 0 10px 22px rgba(36, 63, 107, 0.12);

    &:hover:not(:disabled) {
      border-color: rgba(36, 63, 107, 0.28);
      box-shadow: 0 12px 26px rgba(36, 63, 107, 0.18);
      transform: translateY(-1px);
    }
  }

  .review-mode-toggle {
    display: inline-flex;
    align-self: flex-start;
    gap: 0.4rem;
    padding: 0.25rem;
    border: 1px solid var(--review-line);
    border-radius: 999px;
    background: rgba(255, 255, 255, 0.78);
  }

  .review-sticky-stack {
    position: sticky;
    top: var(--review-sticky-top);
    z-index: 20;
    width: 100%;
    padding: 0.75rem 0 0.85rem;
    background: linear-gradient(
      180deg,
      rgba(243, 246, 251, 0.98) 0%,
      rgba(243, 246, 251, 0.94) 72%,
      rgba(243, 246, 251, 0) 100%
    );
    margin-bottom: 1.25rem;
  }

  .review-mode-toggle-row {
    display: flex;
    justify-content: flex-end;
    width: 100%;
    margin-bottom: 0.85rem;
  }

  .badge-toggle-btn {
    position: relative;
  }

  .pending-badge {
    position: absolute;
    top: -0.45rem;
    right: -0.45rem;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-width: 1.25rem;
    height: 1.25rem;
    padding: 0 0.3rem;
    border-radius: 999px;
    background: var(--danger);
    color: #ffffff;
    font-size: 0.72rem;
    font-weight: 900;
  }

  .pending-badge--dot {
    min-width: 1.05rem;
    height: 1.05rem;
    padding: 0;
  }

  .workbench-layout {
    display: grid;
    grid-template-columns: minmax(290px, 340px) minmax(0, 1fr);
    gap: 1.25rem;
    align-items: start;
  }

  .control-panel {
    position: sticky;
    top: var(--review-sticky-secondary-top);
    z-index: 5;
    padding: 1.15rem;
    display: flex;
    flex-direction: column;
    gap: 1rem;
    background: #f9fafb;
    border-color: var(--review-line);
  }

  .calendar-panel {
    position: relative;
    z-index: 1;
    min-width: 0;
  }

  .panel-section,
  .venue-summary {
    display: flex;
    flex-direction: column;
    gap: 0.45rem;
  }

  .panel-section {
    h2 {
      color: var(--review-ink);
      font-size: var(--text-xl);
    }

    label,
    .section-label {
      color: var(--review-muted);
      font-size: var(--text-sm);
      font-weight: 800;
    }

    select {
      min-height: 3rem;
      padding: 0.75rem 0.95rem;
      border: 1px solid var(--review-line);
      border-radius: var(--radius-sm);
      background: #ffffff;
      color: var(--review-ink);
    }
  }

  .status-filter-section {
    gap: 0.65rem;
  }

  .status-filter-list {
    display: flex;
    flex-direction: column;
    gap: 0.65rem;
  }

  .status-filter-card {
    width: 100%;
    min-height: 5.2rem;
    padding: 0.8rem 0.75rem;
    border: 1px solid var(--review-line);
    border-left: 5px solid #7a8796;
    border-radius: var(--radius-sm);
    display: grid;
    grid-template-columns: auto minmax(0, 1fr) auto;
    align-items: center;
    gap: 0.75rem;
    background: #ffffff;
    color: var(--review-ink);
    text-align: left;
    cursor: pointer;
    transition:
      background-color 0.2s ease,
      border-color 0.2s ease,
      box-shadow 0.2s ease,
      transform 0.2s ease;

    &:hover,
    &:focus-visible {
      background: #f7f8fa;
      border-color: #bfc8d4;
      box-shadow: 0 8px 18px rgba(15, 23, 42, 0.08);
      outline: none;
      transform: translateY(-1px);
    }

    &.is-active {
      background: #eef1f5;
      border-color: #aeb9c7;
      box-shadow: inset 0 0 0 1px rgba(32, 41, 54, 0.08);
    }

    &.is-pending {
      border-left-color: var(--status-pending);
    }

    &.is-approved {
      border-left-color: var(--status-approved);
    }

    &.is-rejected {
      border-left-color: var(--status-rejected);
    }
  }

  .status-filter-icon {
    width: 2.4rem;
    height: 2.4rem;
    border-radius: 12px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    background: #eef1f5;
    color: var(--review-ink);
  }

  .status-filter-card.is-active .status-filter-icon {
    background: #ffffff;
  }

  .status-filter-copy {
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 0.18rem;

    strong {
      color: var(--review-ink);
      font-size: var(--text-base);
      line-height: 1.3;
    }

    span {
      color: var(--review-muted);
      font-size: var(--text-sm);
      line-height: 1.35;
    }
  }

  .status-filter-count {
    min-width: 2.3rem;
    height: 2.3rem;
    padding: 0 0.55rem;
    border-radius: 999px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    background: #d9e2ee;
    color: var(--review-ink);
    font-size: var(--text-lg);
    font-weight: 800;
  }

  .venue-summary {
    padding: 0.9rem;
    border: 1px solid var(--review-line);
    border-radius: var(--radius-sm);
    background: #ffffff;

    strong {
      color: var(--review-ink);
      font-size: var(--text-lg);
      line-height: 1.3;
    }
  }

  .summary-label,
  .summary-subtle {
    color: var(--review-muted);
    font-size: var(--text-sm);
  }

  .booking-entry-btn {
    width: 100%;
  }

  .calendar-panel {
    min-width: 0;
  }

  .panel-heading {
    margin-bottom: 0;
    padding: 0;
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    gap: 1rem;

    h2 {
      margin: 0;
      color: var(--review-ink);
    }
  }

  .panel-note {
    margin: 0.35rem 0 0;
    color: var(--review-muted);
    font-size: var(--text-sm);
    line-height: 1.45;
  }

  .panel-heading-actions {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    flex-wrap: wrap;
    gap: 0.75rem;
  }

  .route-booking-btn {
    flex-shrink: 0;
    margin-top: auto;
  }

  .view-toggle {
    min-height: 2.5rem;
    padding: 0.2rem;
    display: inline-grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 0.2rem;
    border: 1px solid var(--review-line);
    border-radius: 999px;
    background: #eef1f5;
  }

  .view-toggle-btn {
    min-width: 5rem;
    min-height: 2.05rem;
    padding: 0.35rem 0.75rem;
    border: 0;
    border-radius: 999px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 0.4rem;
    background: transparent;
    color: var(--review-muted);
    font-size: var(--text-sm);
    font-weight: 800;
    cursor: pointer;
    transition:
      background-color 0.2s ease,
      color 0.2s ease,
      box-shadow 0.2s ease;

    &.is-active {
      background: #ffffff;
      color: var(--review-ink);
      box-shadow: 0 2px 8px rgba(15, 23, 42, 0.08);
    }
  }

  .quick-status-filter {
    flex-shrink: 0;
    min-height: 2.7rem;
    padding: 0.25rem 0.35rem 0.25rem 0.8rem;
    border: 1px solid var(--review-line);
    border-radius: 999px;
    display: inline-flex;
    align-items: center;
    gap: 0.45rem;
    background: #eef1f5;
    color: var(--review-muted);
    font-size: var(--text-sm);
    font-weight: 800;

    select {
      min-height: 2.05rem;
      padding: 0.25rem 2rem 0.25rem 0.7rem;
      border: 0;
      border-radius: 999px;
      background: #ffffff;
      color: var(--review-ink);
      font-size: var(--text-base);
      font-weight: 800;
      cursor: pointer;

      &:disabled {
        cursor: progress;
      }
    }
  }

  .calendar-shell,
  .list-shell {
    background: var(--review-panel);
    padding: 1.2rem;
    border-radius: var(--radius);
    border: 1px solid var(--review-line);
    box-shadow: var(--shadow-soft);
    overflow-x: auto;
    transition: opacity 0.3s ease;

    &.is-loading {
      opacity: 0.55;
      pointer-events: none;
    }
  }

  .calendar-shell {
    position: relative;
  }

  .list-shell {
    overflow: hidden;
  }

  .month-picker-popover {
    position: absolute;
    top: 4.1rem;
    left: 50%;
    z-index: 20;
    transform: translateX(-50%);
    padding: 0.65rem;
    border: 1px solid var(--review-line);
    border-radius: var(--radius-sm);
    display: flex;
    align-items: center;
    gap: 0.45rem;
    background: #ffffff;
    box-shadow: var(--shadow);

    label {
      color: var(--review-muted);
      font-size: var(--text-sm);
      font-weight: 800;
      white-space: nowrap;
    }

    input {
      width: 9.75rem;
      min-height: 2.35rem;
      padding: 0.35rem 0.55rem;
      border: 1px solid var(--review-line);
      border-radius: 8px;
      background: #ffffff;
      color: var(--review-ink);
      font-weight: 800;
    }
  }

  .month-picker-action {
    min-height: 2.35rem;
    padding: 0.35rem 0.75rem;
    border: 1px solid var(--review-line);
    border-radius: 999px;
    background: #ffffff;
    color: var(--review-ink);
    font-size: var(--text-sm);
    font-weight: 800;
    white-space: nowrap;
    cursor: pointer;

    &:hover {
      background: var(--surface-muted);
    }

    &.is-primary {
      border-color: var(--accent);
      background: var(--accent);
      color: #ffffff;

      &:hover {
        border-color: var(--accent-hover);
        background: var(--accent-hover);
      }
    }
  }

  .list-empty-state {
    min-height: 18rem;
    display: flex;
    align-items: center;
    justify-content: center;
    border: 1px dashed var(--review-line);
    border-radius: var(--radius-sm);
    background: #f7f8fa;
    color: var(--review-muted);
    font-weight: 700;
  }

  .case-list {
    display: flex;
    flex-direction: column;
    border: 1px solid var(--review-line);
    border-radius: var(--radius-sm);
    overflow: hidden;
    background: #ffffff;
  }

  .case-row {
    width: 100%;
    padding: 1.25rem 1.5rem;
    border: 0;
    border-bottom: 1px solid rgba(var(--blue-900-rgb), 0.08);
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 1rem;
    background: #ffffff;
    color: inherit;
    text-align: left;
    cursor: pointer;
    transition:
      background-color 0.2s ease,
      box-shadow 0.2s ease;

    &:last-child {
      border-bottom: 0;
    }

    &:hover,
    &:focus-visible {
      background: #f7f8fa;
      box-shadow: inset 4px 0 0 #202936;
      outline: none;
    }
  }

  .standalone-equipment-panel {
    padding: 1.15rem;
    background: #f9fafb;
    border-color: var(--review-line);
  }

  .equipment-status-filter-section {
    margin-bottom: 0;
  }

  .review-filter-panel {
    padding: 1rem;
    border: 1px solid rgba(var(--blue-900-rgb), 0.08);
    border-radius: calc(var(--radius-sm) + 4px);
    background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 250, 252, 0.94));
  }

  .review-filter-panel--inline {
    margin-bottom: 1rem;
  }

  .review-filter-toolbar {
    display: grid;
    gap: 0.9rem;
  }

  .filter-field,
  .review-date-range-picker {
    display: grid;
    gap: 0.4rem;

    label {
      font-size: var(--text-sm);
      font-weight: 700;
      color: var(--review-ink);
    }

    input {
      min-height: 2.8rem;
      padding: 0.72rem 0.85rem;
      border: 1px solid rgba(var(--blue-900-rgb), 0.14);
      border-radius: var(--radius-sm);
      background: #fff;
      color: var(--review-ink);
    }
  }

  .date-range-picker {
    position: relative;
  }

  .date-range-trigger {
    width: 100%;
    display: grid;
    grid-template-columns: minmax(0, 1fr) minmax(0, 1fr) auto;
    align-items: center;
    gap: 0.75rem;
    padding: 0.75rem 0.85rem;
    border: 1px solid rgba(var(--blue-900-rgb), 0.14);
    border-radius: var(--radius-sm);
    background: #fff;
    color: var(--review-ink);
    text-align: left;

    &.is-open {
      border-color: rgba(var(--blue-900-rgb), 0.3);
      box-shadow: 0 0 0 3px rgba(var(--blue-900-rgb), 0.08);
    }
  }

  .date-range-segment {
    display: grid;
    gap: 0.18rem;

    strong {
      font-size: var(--text-sm);
      font-weight: 800;
    }

    &.has-value strong {
      color: var(--review-ink);
    }
  }

  .date-range-label,
  .calendar-selection-label,
  .calendar-selection-hint,
  .summary-label {
    font-size: var(--text-xs);
    color: var(--review-muted);
  }

  .date-range-chevron {
    color: var(--review-muted);
  }

  .date-range-clear,
  .clear-filter-btn {
    width: fit-content;
    padding: 0;
    border: 0;
    background: transparent;
    color: var(--blue-700);
    font-size: var(--text-xs);
    font-weight: 700;
    cursor: pointer;
  }

  .filter-summary {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 0.75rem;

    strong {
      color: var(--review-ink);
      font-size: var(--text-sm);
    }
  }

  .date-range-popover {
    position: fixed;
    z-index: 60;
    min-width: 18rem;
    max-width: min(24rem, calc(100vw - 1.5rem));
    padding: 0.95rem;
    border: 1px solid rgba(var(--blue-900-rgb), 0.1);
    border-radius: calc(var(--radius-sm) + 4px);
    background: rgba(255, 255, 255, 0.98);
    box-shadow: 0 22px 48px rgba(20, 35, 58, 0.16);
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
  }

  .calendar-selection-footer {
    display: grid;
    gap: 0.9rem;
  }

  .calendar-selection-summary {
    display: grid;
    gap: 0.22rem;

    strong {
      color: var(--review-ink);
    }
  }

  .calendar-manual-inputs {
    display: grid;
    gap: 0.75rem;
  }

  .calendar-manual-field {
    display: grid;
    gap: 0.35rem;

    span {
      font-size: var(--text-xs);
      color: var(--review-muted);
    }
  }

  .equipment-case-list {
    display: flex;
    flex-direction: column;
    gap: 1rem;
    border: 0;
    background: transparent;
    overflow: visible;
  }

  .equipment-case-row {
    grid-template-columns: minmax(0, 1.8fr) minmax(10.5rem, 12rem) minmax(7.25rem, 8.5rem);
    gap: 1rem;
    align-items: start;
    padding: 1rem 1.1rem;
    border: 1px solid rgba(var(--blue-900-rgb), 0.12);
    border-radius: calc(var(--radius-sm) + 4px);
    background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 250, 252, 0.98));
    box-shadow: 0 12px 28px rgba(20, 35, 58, 0.06);
    cursor: default;

    &:hover,
    &:focus-visible {
      background: linear-gradient(180deg, rgba(255, 255, 255, 1), rgba(247, 250, 252, 1));
      box-shadow: 0 16px 34px rgba(20, 35, 58, 0.08);
    }
  }

  .equipment-case-main {
    display: grid;
    grid-template-columns: minmax(0, 1fr) auto;
    grid-template-areas:
      "title meta"
      "facts facts"
      "purpose purpose"
      "contact contact";
    gap: 0.7rem 1rem;
  }

  .equipment-title-line {
    grid-area: title;
    align-items: flex-start;
    margin: 0;

    strong {
      font-size: 1.08rem;
      line-height: 1.35;
    }
  }

  .equipment-card-meta {
    grid-area: meta;
    justify-self: end;
    align-self: start;
  }

  .equipment-card-id {
    display: inline-flex;
    align-items: center;
    min-height: 1.9rem;
    padding: 0.2rem 0.65rem;
    border-radius: 999px;
    background: rgba(var(--blue-900-rgb), 0.06);
    color: var(--review-muted);
    font-size: var(--text-xs);
    font-weight: 800;
    white-space: nowrap;
  }

  .equipment-key-facts {
    grid-area: facts;
    display: grid;
    gap: 0.55rem;
  }

  .equipment-fact {
    min-width: 0;
    display: grid;
    grid-template-columns: 5.25rem minmax(0, 1fr);
    column-gap: 0.85rem;
    row-gap: 0.2rem;
    align-items: start;

    strong,
    .equipment-fact-value {
      color: var(--review-ink);
      font-size: 0.98rem;
      font-weight: 700;
      line-height: 1.35;
      overflow-wrap: anywhere;
    }
  }

  .equipment-fact-full {
    padding: 0;
  }

  .equipment-fact-label,
  .equipment-schedule-label {
    color: var(--review-muted);
    font-size: var(--text-xs);
    font-weight: 800;
    letter-spacing: 0.04em;
    text-transform: uppercase;
  }

  .equipment-fact-label {
    grid-column: 1;
    padding-top: 0.18rem;
  }

  .equipment-fact > strong,
  .equipment-fact > .equipment-fact-value,
  .equipment-fact > .equipment-context-chip,
  .equipment-fact > .equipment-fact-value-stack {
    grid-column: 2;
  }

  .equipment-fact-value-stack {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    gap: 0.28rem;
    min-width: 0;
  }

  .equipment-context-chip {
    width: fit-content;
    min-height: 1.7rem;
    padding: 0.2rem 0.55rem;
    border-radius: 999px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    font-size: var(--text-xs);
    font-weight: 800;
    line-height: 1;

    &.is-linked {
      background: rgba(var(--blue-900-rgb), 0.1);
      color: var(--accent);
    }

    &.is-standalone {
      background: rgba(97, 117, 138, 0.12);
      color: #4d5e71;
    }
  }

  .equipment-fact-value-group {
    grid-column: 2;
    display: flex;
    align-items: center;
    gap: 0.5rem;
    flex-wrap: wrap;
  }

  .equipment-purpose {
    grid-area: purpose;
    margin: 0;
  }

  .equipment-contact-list {
    grid-area: contact;
    display: grid;
    gap: 0.45rem;
  }

  .equipment-contact-row {
    display: grid;
    grid-template-columns: 5.25rem minmax(0, 1fr);
    column-gap: 0.85rem;
    align-items: start;
  }

  .equipment-secondary-meta {
    display: none;
  }

  .equipment-schedule {
    min-width: 0;
    padding: 0.85rem 0.9rem;
    border: 1px solid rgba(var(--blue-900-rgb), 0.16);
    border-radius: var(--radius-sm);
    background:
      linear-gradient(135deg, rgba(39, 94, 168, 0.14), rgba(255, 255, 255, 0.96) 52%),
      linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(39, 94, 168, 0.08));
    box-shadow: 0 8px 20px rgba(39, 94, 168, 0.08);
    align-self: start;
    align-items: flex-start;
    justify-content: flex-start;
    gap: 0.5rem;
    text-align: left;

    strong {
      line-height: 1.3;
    }
  }

  .equipment-schedule-header {
    display: flex;
    align-items: center;
    justify-content: flex-start;
    gap: 0.45rem;
    width: 100%;
    text-align: left;
  }

  .equipment-schedule-badge {
    width: fit-content;
    display: inline-flex;
    align-items: center;
    gap: 0.4rem;
    min-height: 1.95rem;
    padding: 0.3rem 0.65rem;
    border-radius: 999px;
    background: rgba(255, 255, 255, 0.92);
    color: var(--accent);
    font-size: var(--text-sm);
    font-weight: 800;
    letter-spacing: 0.02em;
  }

  .equipment-schedule-date {
    display: block;
    color: var(--review-ink);
    font-size: 1.05rem;
    font-weight: 800;
    letter-spacing: 0.01em;
    width: 100%;
    text-align: left;
  }

  .equipment-time-pill {
    width: fit-content;
    max-width: 100%;
    min-height: 2rem;
    display: inline-flex;
    align-items: center;
    gap: 0.4rem;
    padding: 0.3rem 0.7rem;
    border-radius: 999px;
    background: rgba(255, 255, 255, 0.9);
    color: var(--accent);
    font-size: var(--text-sm);
    font-weight: 800;
    line-height: 1.3;
    text-align: left;
    justify-content: flex-start;
  }

  .equipment-review-row-actions {
    display: flex;
    flex-direction: column;
    align-items: stretch;
    justify-content: flex-start;
    gap: 0.55rem;

    .btn {
      width: 100%;
      justify-content: center;
      min-height: 2.5rem;
      padding-inline: 0.85rem;
    }
  }

  .case-main,
  .case-schedule {
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 0.45rem;
  }

  .case-title-line {
    display: flex;
    align-items: center;
    gap: 0.65rem;
    min-width: 0;

    strong {
      min-width: 0;
      color: var(--review-ink);
      font-size: var(--text-base);
      overflow-wrap: anywhere;
    }
  }

  .case-schedule-right {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    gap: 0.4rem;
    text-align: right;
    flex-shrink: 0;
  }

  .schedule-date {
    color: var(--review-ink);
    font-size: 1.05rem;
    font-weight: 800;
  }

  .schedule-time {
    color: var(--review-muted);
    font-size: 0.95rem;
    font-weight: 600;
  }

  .case-meta {
    display: flex;
    flex-wrap: wrap;
    gap: 0.45rem 0.8rem;
    color: var(--review-muted);
    font-size: var(--text-sm);
    font-weight: 700;
  }

  .case-meta-strong {
    color: var(--review-ink);
    font-weight: 800;
  }

  .case-related-booking {
    width: 100%;
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 0.45rem 0.8rem;
  }

  .case-related-booking-icon {
    width: 1.5rem;
    height: 1.5rem;
    border-radius: 999px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    background: rgba(var(--blue-900-rgb), 0.08);
    color: var(--accent);
    flex-shrink: 0;
  }

  .case-id-pill {
    width: fit-content;
    min-height: 1.75rem;
    padding: 0.18rem 0.6rem;
    border-radius: 999px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    background: rgba(var(--blue-900-rgb), 0.08);
    color: var(--review-ink);
    font-size: var(--text-xs);
    font-weight: 800;
    line-height: 1.2;
    white-space: nowrap;
  }

  .case-id-pill--equipment {
    background: #ffffff;
  }

  .case-schedule {
    align-items: flex-end;

    strong {
      color: var(--review-ink);
    }

    span {
      color: var(--review-muted);
      font-size: var(--text-sm);
      font-weight: 700;
      text-align: right;
    }
  }

  &.history-page {
    display: flex;
    flex-direction: column;
    gap: 1.75rem;
  }

  .workbench-layout {
    display: grid;
    grid-template-columns: minmax(320px, 360px) minmax(0, 1fr);
    gap: 1.5rem;
    align-items: start;
    min-width: 0;
  }

  .workbench-layout > * {
    width: 100%;
    max-width: 100%;
    min-width: 0;
  }

  .control-panel {
    position: sticky;
    top: calc(var(--header-height) + 2.25rem);
    padding: 1.5rem;
    gap: 1.1rem;
    background:
      linear-gradient(180deg, rgba(232, 240, 250, 0.95), rgba(255, 255, 255, 0.98)),
      radial-gradient(circle at top right, rgba(var(--blue-900-rgb), 0.1), transparent 38%);
    border: 1px solid rgba(var(--blue-900-rgb), 0.12);
  }

  .calendar-panel {
    padding: 1.4rem;
    width: 100%;
    max-width: 100%;
    border-radius: calc(var(--radius-lg) + 2px);
    background: rgba(255, 255, 255, 0.72);
    border: 1px solid rgba(var(--blue-900-rgb), 0.08);
    box-shadow: var(--shadow-soft);
  }

  .standalone-equipment-panel {
    padding: 1.4rem;
    background: rgba(255, 255, 255, 0.72);
    border-color: rgba(var(--blue-900-rgb), 0.08);
  }

  .filter-panel,
  .review-filter-panel {
    display: flex;
    flex-direction: column;
    gap: 0;
    padding: 0;
    margin-bottom: 0;
    border: 0;
    background: transparent;
  }

  .review-filter-toolbar {
    display: grid;
    grid-template-columns: 1fr;
    gap: 1rem;
    align-items: stretch;
    padding: 1rem 1.2rem;
    border-radius: var(--radius);
    background: rgba(255, 255, 255, 0.72);
    border: 1px solid rgba(var(--blue-900-rgb), 0.08);
    backdrop-filter: blur(8px);
  }

  .filter-field,
  .review-date-range-picker {
    display: flex;
    flex-direction: column;
    gap: 0.45rem;

    label {
      color: var(--muted-strong);
      font-size: var(--text-sm);
      font-weight: 700;
    }

    input,
    select {
      min-height: 2.85rem;
      padding: 0.75rem 0.95rem;
      border: 1px solid var(--line);
      border-radius: var(--radius-sm);
      background: #ffffff;
      color: var(--ink);
    }
  }

  .date-range-trigger {
    position: relative;
    display: grid;
    grid-template-columns: minmax(0, 1fr) minmax(0, 1fr) auto;
    align-items: stretch;
    width: 100%;
    min-height: 4.35rem;
    padding: 0;
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    background: #ffffff;
    color: var(--ink);
    text-align: left;
    cursor: pointer;
    overflow: hidden;
  }

  .date-range-segment {
    display: flex;
    flex-direction: column;
    justify-content: center;
    min-width: 0;
    padding: 0.75rem 0.9rem;

    & + & {
      border-left: 1px solid rgba(var(--blue-900-rgb), 0.1);
    }

    strong {
      margin-top: 0.3rem;
      color: var(--muted-strong);
      font-size: var(--text-base);
      line-height: 1.2;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
  }

  .date-range-chevron {
    align-self: center;
    margin-right: 0.85rem;
    color: var(--accent);
  }

  .date-range-clear,
  .clear-filter-btn {
    align-self: flex-start;
    padding: 0;
    border: 0;
    background: none;
    color: var(--accent);
    font-size: var(--text-sm);
    font-weight: 800;
    cursor: pointer;

    &:hover {
      color: var(--accent-hover);
      text-decoration: underline;
    }
  }

  .filter-summary {
    align-items: flex-start;
    flex-direction: column;
    justify-content: flex-start;
    gap: 0.35rem;

    strong {
      color: var(--ink);
      font-size: var(--text-xl);
      line-height: 1.2;
    }
  }

  .status-filter-section {
    gap: 0.8rem;
  }

  .status-filter-list {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 1rem;
  }

  .status-filter-card {
    min-height: auto;
    padding: 1.1rem 1.2rem;
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    gap: 0.4rem;
    border: 1px solid rgba(var(--blue-900-rgb), 0.06);
    border-radius: var(--radius);
    background: rgba(255, 255, 255, 0.88);
    box-shadow: none;

    &:hover,
    &:focus-visible,
    &.is-active {
      background: rgba(255, 255, 255, 0.96);
      border-color: rgba(var(--blue-900-rgb), 0.14);
      box-shadow: 0 10px 24px rgba(39, 94, 168, 0.08);
      transform: translateY(-1px);
    }
  }

  .status-filter-icon {
    display: none;
  }

  .status-filter-copy {
    span {
      display: none;
    }
  }

  .status-filter-count {
    min-width: auto;
    height: auto;
    padding: 0;
    background: transparent;
    color: var(--ink);
    font-size: var(--text-2xl);
    line-height: 1;
  }

  .filter-tabs {
    display: flex;
    align-items: center;
    gap: 1rem;
    padding: 0 0 0.25rem;
    border-bottom: 1px solid rgba(var(--blue-900-rgb), 0.1);
    overflow-x: auto;
    scrollbar-width: none;

    &::-webkit-scrollbar {
      display: none;
    }
  }

  .record-tabs {
    margin: -0.15rem -0.1rem 1.1rem;
    padding: 0 0.1rem 0.25rem;
  }

  .filter-tab {
    display: inline-flex;
    align-items: center;
    gap: 0.45rem;
    position: relative;
    padding: 0.6rem 0.25rem 0.9rem;
    border: 0;
    background: transparent;
    color: var(--ink);
    font-size: clamp(1.1rem, 1.2vw, 1.35rem);
    font-weight: 800;
    white-space: nowrap;
    cursor: pointer;
    transition: color 0.2s ease;

    &::after {
      content: "";
      position: absolute;
      left: 0;
      right: 0;
      bottom: -0.26rem;
      height: 4px;
      border-radius: 999px;
      background: transparent;
      transition: background-color 0.2s ease;
    }

    &:hover,
    &.is-active {
      color: var(--accent);
    }

    &.is-active::after {
      background: var(--accent);
    }
  }

  .calendar-shell,
  .list-shell {
    padding: 0;
    border: 0;
    border-radius: 0;
    background: transparent;
    box-shadow: none;
    overflow: visible;
  }

  .case-list {
    display: flex;
    flex-direction: column;
    border: 1px solid var(--review-line);
    border-radius: var(--radius-sm);
    overflow: hidden;
    background: #ffffff;
    gap: 0;
    box-shadow: var(--shadow-soft);
  }

  .case-row {
    padding: 1.35rem 1.5rem;
    border: 0;
    border-bottom: 1px solid rgba(var(--blue-900-rgb), 0.08);
    border-radius: 0;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 1.25rem;
    background: transparent;
    box-shadow: none;

    &:last-child {
      border-bottom: 0;
    }

    &:hover,
    &:focus-visible {
      background: #fcfcfc;
      box-shadow: inset 4px 0 0 #202936;
      transform: none;
    }
  }

  .case-main {
    flex: 1;
    min-width: 0;
  }

  .case-title-line {
    align-items: flex-start;
    justify-content: flex-start;
    gap: 1rem;
    margin-bottom: 0.5rem;

    strong {
      color: var(--ink);
      font-size: clamp(1.1rem, 1.2vw, 1.35rem);
      line-height: 1.35;
    }
  }

  .case-meta,
  .case-related-booking {
    gap: 0.65rem;
  }

  .case-id-pill {
    background: rgba(var(--blue-900-rgb), 0.06);
    color: var(--accent);
  }

  .case-schedule {
    min-width: 13rem;
    padding: 1rem 1.1rem;
    border-radius: var(--radius-sm);
    background:
      linear-gradient(135deg, rgba(39, 94, 168, 0.14), rgba(255, 255, 255, 0.96) 52%),
      linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(39, 94, 168, 0.08));
    border: 1px solid rgba(39, 94, 168, 0.22);
    box-shadow: 0 12px 28px rgba(39, 94, 168, 0.08);
    align-items: flex-start;
    text-align: left;

    span {
      text-align: left;
    }
  }

  .pagination-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 1rem;
    margin-top: 1.1rem;
    padding-top: 1rem;
    border-top: 1px solid rgba(var(--blue-900-rgb), 0.08);
  }

  .pagination-summary {
    margin: 0;
    color: var(--muted-strong);
    font-size: var(--text-sm);
    font-weight: 700;
  }

  .pagination-controls {
    display: inline-flex;
    align-items: center;
    gap: 0.4rem;
  }

  .pagination-btn,
  .pagination-page {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-width: 2.25rem;
    height: 2.25rem;
    border: 1px solid rgba(var(--blue-900-rgb), 0.12);
    border-radius: var(--radius-sm);
    background: rgba(255, 255, 255, 0.88);
    color: var(--ink);
    font-weight: 800;
    cursor: pointer;

    &:hover:not(:disabled):not(.is-active) {
      border-color: rgba(var(--blue-900-rgb), 0.22);
      background: rgba(var(--blue-900-rgb), 0.06);
      color: var(--accent);
      transform: translateY(-1px);
    }

    &:disabled {
      color: var(--muted);
      cursor: not-allowed;
      opacity: 0.48;
    }
  }

  .pagination-page {
    padding: 0 0.75rem;
    font-size: var(--text-sm);

    &.is-active {
      border-color: var(--accent);
      background: var(--accent);
      color: #ffffff;
      cursor: default;
    }
  }


  :deep(.fc) {
    min-width: 760px;

    .fc-toolbar.fc-header-toolbar {
      justify-content: center;
      margin-bottom: 1.25rem;
    }

    .fc-toolbar-chunk {
      display: flex;
      align-items: center;
      gap: 1rem;
    }

    .fc-toolbar-title {
      color: var(--review-ink);
      font-size: 1.35rem;
      font-weight: 800;

      &.is-month-picker-trigger {
        padding: 0.25rem 0.55rem;
        border-radius: 999px;
        cursor: pointer;
        transition:
          background-color 0.2s ease,
          box-shadow 0.2s ease;

        &:hover,
        &:focus-visible {
          background: #eef1f5;
          box-shadow: inset 0 0 0 1px var(--review-line);
          outline: none;
        }
      }
    }

    .fc-button-primary {
      font-size: 0.8rem;
      color: var(--review-ink);
      background-color: #eef1f5;
      border: 1px solid var(--review-line);
      border-radius: 999px;

      &:hover,
      &:focus {
        background-color: #202936;
        color: #ffffff;
      }
    }

    .fc-scrollgrid,
    .fc-theme-standard td,
    .fc-theme-standard th {
      border-color: var(--review-line);
    }

    .fc-col-header-cell {
      background: #f4f6f8;
      color: #5b6675;
      font-size: var(--text-sm);
    }

    .fc-daygrid-day {
      cursor: pointer;
      background: #ffffff;
      transition: background-color 0.2s ease;

      &:hover {
        background-color: #f7f8fa;
      }
    }

    .fc-event {
      cursor: pointer;
      border-radius: 8px;
      padding: 4px 5px;
      font-size: 0.78rem;
      transition:
        filter 0.2s ease,
        box-shadow 0.2s ease,
        transform 0.2s ease;

      &:hover {
        filter: brightness(0.98);
        box-shadow: 0 2px 8px rgba(15, 23, 42, 0.14);
        transform: translateY(-1px);
      }
    }

    .fc-daygrid-more-link {
      display: inline-flex;
      align-items: center;
      margin-top: 0.35rem;
      color: var(--review-ink);
      cursor: default;
      font-size: 0.95rem;
      font-weight: 800;
      text-decoration: none;
      pointer-events: none;
    }

    .fc-daygrid-day-top {
      position: relative;
      display: flex;
      justify-content: flex-start;
      align-items: center;
      padding: 0.25rem 0.35rem;
      min-height: 2rem;
    }

    .fc-daygrid-day-number {
      width: 100%;
      padding: 0;
      text-align: left;
      line-height: 1;
      text-decoration: none;
    }

    .calendar-day-number {
      display: block;
      width: 100%;
      color: var(--review-ink);
      font-size: 1.05rem;
      font-weight: 800;
      line-height: 1;
      text-align: left;
    }

    .calendar-day-count {
      position: absolute;
      top: 50%;
      right: 0.35rem;
      transform: translateY(-50%);
      min-width: 1.55rem;
      height: 1.55rem;
      padding: 0 0.45rem;
      border-radius: 999px;
      background-color: var(--accent);
      color: #ffffff;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      font-size: 0.8rem;
      font-weight: 800;
      line-height: 1;
      z-index: 0;
    }

    .calendar-event-content {
      display: flex;
      flex-direction: column;
      gap: 2px;
      line-height: 1.2;
    }

    .calendar-event-status {
      width: max-content;
      max-width: 100%;
      padding: 0.16rem 0.36rem;
      border-radius: 999px;
      background: rgba(255, 255, 255, 0.78);
      color: #17202c;
      font-size: 0.7rem;
      font-weight: 800;
    }

    .calendar-event-time {
      font-weight: 800;
    }

    .calendar-event-purpose,
    .calendar-event-time {
      display: block;
      white-space: normal;
      word-break: break-word;
    }
  }

  @media (max-width: 1024px) {
    .review-sticky-stack {
      position: static;
      top: auto;
      padding: 0;
      background: transparent;
    }

    .workbench-layout {
      grid-template-columns: 1fr;
    }

    .control-panel {
      position: static;
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      align-items: end;
    }

    .panel-section:first-child,
    .status-filter-section {
      grid-column: 1 / -1;
    }

    .equipment-case-row {
      grid-template-columns: minmax(0, 1fr) minmax(9.5rem, 11rem);
    }

    .equipment-review-row-actions {
      grid-column: 1 / -1;
      flex-direction: row;
      justify-content: flex-start;

      .btn {
        width: auto;
        min-width: 7.5rem;
      }
    }

  }

  @media (max-width: 760px) {
    .workbench-header,
    .panel-heading {
      align-items: stretch;
      flex-direction: column;
    }

    .panel-heading-actions {
      align-items: stretch;
      flex-direction: column;
    }

    .header-actions {
      align-items: stretch;
      margin-left: 0;
    }

    .view-toggle {
      width: 100%;
    }

    .quick-status-filter {
      width: 100%;

      select {
        flex: 1;
      }
    }

    .control-panel {
      grid-template-columns: 1fr;
    }

    .calendar-shell,
    .list-shell {
      padding: 0.75rem;
      border-radius: var(--radius-sm);
    }

    .case-row {
      grid-template-columns: 1fr;
    }

    .equipment-case-main {
      grid-template-columns: 1fr;
      grid-template-areas:
        "title"
        "meta"
        "facts"
        "purpose"
        "contact";
    }

    .equipment-card-meta {
      justify-self: start;
    }

    .case-schedule {
      align-items: flex-start;

      span {
        text-align: left;
      }
    }

    .equipment-schedule {
      min-width: 0;
    }

    .equipment-review-row-actions {
      justify-content: flex-start;
      flex-wrap: wrap;

      .btn {
        width: 100%;
      }
    }
  }
}

</style>
