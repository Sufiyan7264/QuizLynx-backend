package com.projectJava.quizApp.utility;

public class EmailTemplate {

    public static String getContactTemplate(String name, String email, String subject, String messageContent) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f4f9; margin: 0; padding: 0; }
                    .container { max-width: 600px; margin: 30px auto; background-color: #ffffff; border-radius: 12px; box-shadow: 0 4px 15px rgba(0,0,0,0.1); overflow: hidden; }
                    
                    /* CSS Percentages must use double %% */
                    .header { background: linear-gradient(135deg, #6b21a8 0%%, #2563eb 100%%); padding: 30px; text-align: center; color: white; }
                    
                    .header h1 { margin: 0; font-size: 28px; font-weight: 700; letter-spacing: 1px; }
                    .content { padding: 30px; color: #333333; line-height: 1.6; }
                    .field { margin-bottom: 20px; }
                    .label { font-size: 12px; color: #666; text-transform: uppercase; font-weight: bold; margin-bottom: 5px; display: block; }
                    .value { font-size: 16px; font-weight: 500; color: #111; background: #f8f9fa; padding: 10px; border-radius: 6px; border-left: 4px solid #3b82f6; }
                    .message-box { background-color: #f0fdf4; border-left: 4px solid #22c55e; padding: 15px; border-radius: 6px; font-style: italic; color: #166534; }
                    .footer { background-color: #f8fafc; padding: 20px; text-align: center; font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Quizlynx</h1>
                        <p style="margin: 5px 0 0 0; opacity: 0.9;">New User Inquiry</p>
                    </div>
                    <div class="content">
                        <p style="font-size: 18px; margin-top: 0;">Hello Admin,</p>
                        <p>You have received a new message via the <strong>Contact Us</strong> form.</p>
                        
                        <div class="field">
                            <span class="label">Sender Name</span>
                            <div class="value">%s</div>
                        </div>

                        <div class="field">
                            <span class="label">Sender Email</span>
                            <div class="value">%s</div>
                        </div>
                        
                        <div class="field">
                            <span class="label">Subject</span>
                            <div class="value">%s</div>
                        </div>

                        <div class="field">
                            <span class="label">Message Content</span>
                            <div class="message-box">
                                "%s"
                            </div>
                        </div>
                        
                        <p style="margin-top: 30px; font-size: 14px; color: #666;">
                            You can reply directly to this email to contact the user.
                        </p>
                    </div>
                    <div class="footer">
                        &copy; 2025 Quizlynx System. Automated Message.
                    </div>
                </div>
            </body>
            </html>
            """.formatted(name, email, subject, messageContent);
    }
}