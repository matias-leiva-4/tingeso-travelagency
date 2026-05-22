import httpClient from "../http-common";

const getMe = () => httpClient.get("/api/users/me");
const updateProfile = (data) => httpClient.put("/api/users/me/profile", data);

export default { getMe, updateProfile };
