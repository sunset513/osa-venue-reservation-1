import request from "./index";

export const fetchCurrentUser = () => {
  return request.get("/me");
};
