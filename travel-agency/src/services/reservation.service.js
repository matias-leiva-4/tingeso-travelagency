import httpClient from "../http-common";

const create = (data) => httpClient.post("/api/reservations", data);
const getById = (id) => httpClient.get(`/api/reservations/${id}`);
const getByUser = () => httpClient.get("/api/reservations");
const cancel = (id) => httpClient.delete(`/api/reservations/${id}`);
const getReceipt = (id) => httpClient.get(`/api/reservations/${id}/receipt`);

export default { create, getById, getByUser, cancel, getReceipt };
