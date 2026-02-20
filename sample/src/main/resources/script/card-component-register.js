/**
 * Card Component Template Registration
 * This file registers the HTML template and styles for the card-component
 * The server only sends JSON props updates via WebSocket
 */

(function() {
    'use strict';

    // Register the card component template
    registerTemplateComponent('card-component', {
        template: `
            <div class="card {{#highlighted}}highlighted{{/highlighted}}" 
                 style="border-color: {{color}}" 
                 data-event="cardClick">
                <div class="card-header">
                    <h2 class="card-title">{{title}}</h2>
                    <button class="delete-btn" data-event="deleteClick">×</button>
                </div>
                <div class="card-body">
                    <p>{{content}}</p>
                </div>
                <div class="card-footer">
                    <small>Click card to highlight</small>
                </div>
            </div>
        `,
        styles: `
            .card {
                border: 2px solid;
                border-radius: 8px;
                padding: 0;
                margin: 1rem;
                background: white;
                cursor: pointer;
                transition: all 0.3s ease;
                box-shadow: 0 2px 4px rgba(0,0,0,0.1);
            }
            
            .card:hover {
                box-shadow: 0 4px 8px rgba(0,0,0,0.2);
                transform: translateY(-2px);
            }
            
            .card.highlighted {
                box-shadow: 0 0 20px rgba(52, 152, 219, 0.5);
                transform: scale(1.02);
            }
            
            .card-header {
                display: flex;
                justify-content: space-between;
                align-items: center;
                padding: 1rem;
                border-bottom: 1px solid #eee;
            }
            
            .card-title {
                margin: 0;
                font-size: 1.5rem;
                color: #333;
            }
            
            .delete-btn {
                background: none;
                border: none;
                font-size: 2rem;
                color: #999;
                cursor: pointer;
                padding: 0;
                width: 30px;
                height: 30px;
                line-height: 1;
            }
            
            .delete-btn:hover {
                color: #e74c3c;
            }
            
            .card-body {
                padding: 1rem;
            }
            
            .card-body p {
                margin: 0;
                color: #666;
                line-height: 1.6;
            }
            
            .card-footer {
                padding: 0.5rem 1rem;
                background: #f8f9fa;
                border-top: 1px solid #eee;
                border-radius: 0 0 6px 6px;
            }
            
            .card-footer small {
                color: #999;
                font-size: 0.875rem;
            }
        `
    });

    console.log('Card component template registered');

})();
