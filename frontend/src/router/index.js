import { createRouter, createWebHistory } from "vue-router";
import UnitSelector from "@/views/UnitSelector.vue";
import VenueSelector from "@/views/VenueSelector.vue";
import VenueCalendar from "@/views/VenueCalendar.vue";
import MyBookingHistory from "@/views/MyBookingHistory.vue";
import ReviewCalendar from "@/views/ReviewCalendar.vue";
import EquipmentStatus from "@/views/EquipmentStatus.vue";
import EquipmentBorrowHistory from "@/views/EquipmentBorrowHistory.vue";
import ActivityDashboard from "@/views/ActivityDashboard.vue";
import ConsentAgreement from "@/views/ConsentAgreement.vue";
import { validateRouteAccess } from "./guards";

const routes = [
  {
    path: "/",
    name: "Home",
    component: UnitSelector,
  },
  {
    path: "/unit/:unitId",
    name: "VenueSelector",
    component: VenueSelector,
    props: true, // 將路徑參數轉為組件的 props
    meta: {
      validateUnit: true,
      redirectOnInvalid: "/",
    },
  },
  {
    path: "/venue/:venueId",
    name: "VenueCalendar",
    component: VenueCalendar,
    props: true, // 將路徑參數轉為組件的 props
    meta: {
      validateVenue: true,
      redirectOnInvalid: "/",
    },
  },
  {
    path: "/my-bookings",
    name: "MyBookingHistory",
    component: MyBookingHistory,
  },
  {
    path: "/activity-dashboard",
    name: "ActivityDashboard",
    component: ActivityDashboard,
  },
  {
    path: "/equipment-status",
    name: "EquipmentStatus",
    component: EquipmentStatus,
  },
  {
    path: "/equipment-history",
    name: "EquipmentBorrowHistory",
    component: EquipmentBorrowHistory,
  },
  {
    path: "/review",
    name: "ReviewCalendar",
    component: ReviewCalendar,
  },
  {
    path: "/consent-agreement",
    name: "ConsentAgreement",
    component: ConsentAgreement,
    meta: {
      skipConsentGate: true,
    },
  },
];

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
});

router.beforeEach(async (to) => {
  return validateRouteAccess(to);
});

export default router;
