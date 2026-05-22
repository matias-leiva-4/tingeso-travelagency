import httpClient from "../http-common";

const createPackage = (data) => httpClient.post("/api/packages", data);
const updatePackage = (id, data) => httpClient.put(`/api/packages/${id}`, data);
const deletePackage = (id) => httpClient.delete(`/api/packages/${id}`);
const publishPackage = (id) => httpClient.post(`/api/packages/${id}/publish`);
const getAllPackages = () => httpClient.get("/api/packages");
const getSalesReport = (from, to) =>
  httpClient.get("/api/reports/sales", { params: { from, to } });
const getPackageRanking = (from, to) =>
  httpClient.get("/api/reports/packages/ranking", { params: { from, to } });

export default {
  createPackage,
  updatePackage,
  deletePackage,
  publishPackage,
  getAllPackages,
  getSalesReport,
  getPackageRanking,
};
