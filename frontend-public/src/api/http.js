import axios from 'axios'

const http = axios.create({
  baseURL: '/',
  timeout: 20000
})

http.interceptors.response.use(
  (res) => {
    const body = res.data
    if (body && typeof body.code !== 'undefined' && body.code !== 200) {
      return Promise.reject(new Error(body.msg || 'Request failed'))
    }
    return body
  },
  (err) => Promise.reject(err)
)

export default http
