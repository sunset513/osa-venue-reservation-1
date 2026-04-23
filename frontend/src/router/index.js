import { createRouter, createWebHistory } from "vue-router";
import UnitSelector from "@/views/UnitSelector.vue";
import VenueSelector from "@/views/VenueSelector.vue";
import VenueCalendar from "@/views/VenueCalendar.vue";
import MyBookingHistory from "@/views/MyBookingHistory.vue";
import ReviewCalendar from "@/views/ReviewCalendar.vue";
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
    path: "/review",
    name: "ReviewCalendar",
    component: ReviewCalendar,
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach(async (to) => {
  return validateRouteAccess(to);
});

export default router;
