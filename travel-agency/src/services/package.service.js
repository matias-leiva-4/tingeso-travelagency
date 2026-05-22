import httpClient from "../http-common";

function removeEmptyValues(values) {
  return Object.fromEntries(
    Object.entries(values).filter(
      ([, value]) => value !== "" && value !== null && value !== undefined,
    ),
  );
}

const getAvailable = () => httpClient.get("/api/packages/available");
const getAll = () => httpClient.get("/api/packages/search");
const search = (params) =>
  httpClient.get("/api/packages/search", { params: removeEmptyValues(params) });
const getById = (id) => httpClient.get(`/api/packages/${id}`);

export default { getAvailable, getAll, search, getById };
