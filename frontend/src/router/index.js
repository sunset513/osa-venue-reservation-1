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
import NotFound from "@/views/NotFound.vue";
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
    props: true,
    meta: {
      validateUnit: true,
    },
  },
  {
    path: "/venue/:venueId",
    name: "VenueCalendar",
    component: VenueCalendar,
    props: true,
    meta: {
      validateVenue: true,
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
    meta: {
      requiresReviewer: true,
    },
  },
  {
    path: "/equipment-history",
    name: "EquipmentBorrowHistory",
    component: EquipmentBorrowHistory,
    meta: {
      requiresReviewer: true,
    },
  },
  {
    path: "/review",
    name: "ReviewCalendar",
    component: ReviewCalendar,
    meta: {
      requiresReviewer: true,
    },
  },
  {
    path: "/consent-agreement",
    name: "ConsentAgreement",
    component: ConsentAgreement,
  },
  {
    path: "/404",
    name: "NotFound",
    component: NotFound,
  },
  {
    path: "/:pathMatch(.*)*",
    redirect: {
      name: "NotFound",
      replace: true,
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
