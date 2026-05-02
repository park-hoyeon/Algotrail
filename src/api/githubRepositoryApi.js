import axios from "axios";

const API_BASE_URL = "http://localhost:8080/api";

export const getConnectedGithubRepository = async (userId) => {
  const response = await axios.get(
    `${API_BASE_URL}/github/repository/connected`,
    {
      params: { userId },
    }
  );

  return response.data;
};