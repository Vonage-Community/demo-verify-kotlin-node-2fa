const express = require("express");
const cors = require("cors");

const { Auth } = require("@vonage/auth");
const { Verify2 } = require("@vonage/verify2");

require("dotenv").config();

const app = express();

app.use(express.json());
app.use(express.urlencoded({ extended: true }));
app.use(cors());

const credentials = new Auth({
  applicationId: process.env.VONAGE_APP_ID,
  privateKey: process.env.VONAGE_PRIVATE_KEY,
});

const verifyClient = new Verify2(credentials);

app.get("/", (req, res) => {
  res.send("Vonage Verify backend is running.");
});

app.post("/verification", async (req, res) => {
  const { phone } = req.body || null;
  if (!phone) {
    return res.status(400).json({ error: "Phone number is required." });
  }

  console.log("Received verification request: ", phone);

  // Call to Verify2
  try {
    const result = await verifyClient.newRequest({
      brand: "DemoApp",
      workflow: [
        {
          channel: "silent_auth",
          to: phone,
        },
        {
          channel: "sms",
          to: phone,
        },
      ],
    });

    console.log(result);

    return res.json({
      request_id: result.requestId,
      check_url: result.checkUrl,
    });
  } catch (error) {
    console.error(error.response);
    return res.status(error.response.status).json({ error: error.message });
  }
});

app.post("/callback", async (req, res) => {
  console.log("---- Callback ----");
  console.log(req.body);
  console.log("------------------");
  const { request_id, status } = req.body;
  return res.status(200).json({ status: status });
});

app.post("/check-code", async (req, res) => {
  const { request_id, code } = req.body;

  console.log("code", code);
  console.log("request_id", request_id);
  try {
    const result = await verifyClient.checkCode(request_id, code);

    return res.json({
      verified: result === "completed",
    });
  } catch (error) {
    console.error(error);
    return res.status(400).json({ error: error.message });
  }
});

// Start Server
const PORT = process.env.PORT || 4000;
app.listen(PORT, () => {
  console.log(`Listening on port ${PORT}`);
});
