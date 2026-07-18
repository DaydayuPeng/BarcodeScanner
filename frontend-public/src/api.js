import axios from 'axios'

const http = axios.create({ baseURL: '/', timeout: 15000 })

http.interceptors.response.use(
  (res) => {
    const body = res.data
    if (body && body.code !== 200) {
      return Promise.reject(new Error(body.msg || 'Request failed'))
    }
    return body
  },
  (err) => Promise.reject(err)
)

export default http
