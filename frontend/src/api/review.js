import request from "./index";

const ADMIN_AUTH_HEADER = {
  Authorization: "mock-token-admin-123",
};

const withAdminHeaders = (config = {}) => ({
  ...config,
  headers: {
    ...config.headers,
    ...ADMIN_AUTH_HEADER,
  },
});

export const fetchPendingReviews = ({ venueId, startDate, endDate, status }) => {
  const params = {
    venueId,
    startDate,
    endDate,
  };

  if (status !== undefined && status !== null && status !== "") {
    params.status = status;
  }

  return request.get("/reviews/pending", withAdminHeaders({ params }));
};

export const fetchReviewBookingDetail = (bookingId) => {
  return request.get(`/reviews/bookings/${bookingId}`, withAdminHeaders());
};

export const approveReviewBooking = (bookingId) => {
  return request.post(`/reviews/bookings/${bookingId}/approve`, null, withAdminHeaders());
};

export const updateReviewBookingStatus = (bookingId, status) => {
  return request.put(
    `/reviews/bookings/${bookingId}/status`,
    { bookingId, status },
    withAdminHeaders(),
  );
};
