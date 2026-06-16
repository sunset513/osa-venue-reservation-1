import request from "./index";

export const fetchPendingReviews = ({ venueId, startDate, endDate, status }) => {
  const params = {
    venueId,
    startDate,
    endDate,
  };

  if (status !== undefined && status !== null && status !== "") {
    params.status = status;
  }

  return request.get("/reviews/pending", { params });
};

export const fetchReviewBookingDetail = (bookingId) => {
  return request.get(`/reviews/bookings/${bookingId}`);
};

export const approveReviewBooking = (bookingId) => {
  return request.post(`/reviews/bookings/${bookingId}/approve`);
};

export const updateReviewBookingStatus = (bookingId, status) => {
  return request.put(
    `/reviews/bookings/${bookingId}/status`,
    { bookingId, status },
  );
};
