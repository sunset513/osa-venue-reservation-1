import { createRouter, createWebHistory } from "vue-router";
import UnitSelector from "@/views/UnitSelector.vue";
import VenueSelector from "@/views/VenueSelector.vue";
import VenueCalendar from "@/views/VenueCalendar.vue";
import ReviewCalendar from "@/views/ReviewCalendar.vue";

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
  },
  {
    path: "/venue/:venueId",
    name: "VenueCalendar",
    component: VenueCalendar,
    props: true, // 將路徑參數轉為組件的 props
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

export default router;
