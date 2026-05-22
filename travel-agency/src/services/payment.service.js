import httpClient from "../http-common";

const process = (data) => httpClient.post("/api/payments", data);

export default { process };
